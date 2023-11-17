

package com.bbn.marti.service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.cot.filter.VBMSASharingFilter;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;
import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.FixedSizeBlockingQueue;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class BrokerService extends BaseService {
	private static final Logger logger = LoggerFactory.getLogger(BrokerService.class);

	private static BrokerService instance = null;

	public static synchronized BrokerService getInstance() {
		if (instance == null) {
			instance = SpringContextBeanForApi.getSpringContext().getBean(BrokerService.class);
		}

		return instance;
	}

	@Autowired
	private SubscriptionManager subMgr;
	FixedSizeBlockingQueue<CotEventContainer> inputQueue = new FixedSizeBlockingQueue<CotEventContainer>();

	@Autowired
	private StreamingEndpointRewriteFilter streamendpointFilter;
	
	@Autowired
	private SubscriptionStore subscriptionStore;

	@Override
	public String name() {
		return "Broker";
	}

	@Override
	public boolean addToInputQueue(final CotEventContainer c) {
		if (c != null && // not null
				// Brokering context flag is set
				(c.getContextValue(Constants.DO_NOT_BROKER_KEY) == null ||
				// Do NOT broker
				((Boolean) c.getContextValue(Constants.DO_NOT_BROKER_KEY)).booleanValue() != true)) {
			try {

				ExecutorService executorService = c.getContext(Constants.STORE_FORWARD_KEY) != null ?
						Resources.storeForwardChatProcessor : Resources.brokerMatchingProcessor;

				executorService.execute(() -> {

					try {
						streamendpointFilter.filter(c);

						Collection<Subscription> hits = subMgr.getMatches(c);
						
						c.setContextValue(Constants.SUBSCRIBER_HITS_KEY, subscriptionStore.subscriptionCollectionToConnectionIdSet(hits));

						inputQueue.add(c);

					} catch (Exception e) {
						logger.error("exception filtering message and adding to queue", e);
					}
				});
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception scheduling filtering message and adding to queue job", e);
				}
			}					

			return true;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("purposefully not brokering this event.");
		}
		
		return false;
	}

	@Override
	protected void processNextEvent() {
		CotEventContainer c = null;
		try {
			c = inputQueue.take();
			if (c == null) {
				return;
			}
		} catch (InterruptedException e) {
			return;
		}
		
		final CotEventContainer fc = c;

		ExecutorService executorService = fc.getContext(Constants.STORE_FORWARD_KEY) != null ?
				Resources.storeForwardChatProcessor : Resources.messageProcessor;

		try {
			executorService.execute(() -> {
				try {
					processMessage(fc);
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception processing message in broker service", e);
					}
				}
			});
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception submitting broker service process message job", e);
			}
		}
		
		for (final BaseService s : consumers) {
			try {
				// need independent copies of event for each path out
				Resources.messageCopyProcessor.submit(() -> {
					try {
						s.addToInputQueue(fc.copy());
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("exception adding to broker service input queue", e);
						}
					}
				});
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Exception while processing queue " + inputQueue + " element " + c, e);
				}
			}
		}
	}

	public void processMessage(CotEventContainer cot) {
		try {

			if(VBMSASharingFilter.getInstance().filter(cot) == null) return;
			
			long hitTime = System.currentTimeMillis();

			@SuppressWarnings("unchecked")
			Set<String> hits = (Set<String>) cot.getContextValue(Constants.SUBSCRIBER_HITS_KEY);
			Set<String> websocketHits = new ConcurrentSkipListSet<>();
			
			String senderConnectionId = null;
			if (cot.getContextValue(Constants.CONNECTION_ID_KEY) != null) {
				senderConnectionId = (String) cot.getContextValue(Constants.CONNECTION_ID_KEY);
			}			

			// pre-convert to protobuf
			cot.setProtoBufBytes(StreamingProtoBufProtocol.convertCotToProtoBufBytes(cot));
			
			for (String connectionId : hits) {
				// if the message was injected by a plugin, the list of hits may contain the original sender.
				// we can filter them out by tracking the connection id the message originally came from				
				if (cot.getContextValue(Constants.PLUGIN_PROVENANCE) != null && senderConnectionId != null 
						&& senderConnectionId.equals(connectionId)) continue;
				
				Subscription subscription = null;

				try {

					ConnectionInfo conn = new ConnectionInfo();

					conn.setConnectionId(connectionId);

					subscription = subscriptionStore.getSubscriptionByConnectionInfo(conn);

					if (logger.isTraceEnabled()) {
						logger.trace("BrokerService subscription " + (subscription == null ? "null" : subscription.getClass().getSimpleName()) + " to send to: " + subscription);
					}

					if (subscription != null) {
						if (!CollectionUtils.isEmpty(cot.getBinaryPayloads())) {
							if (subscription.isLinkedToWebsocket.get()) {
								websocketHits.add(subscription.linkedWebsocketConnectionId);
							}
						} else if (subscription.isWebsocket.get()) {
							websocketHits.add(connectionId);
							subscription.incHit(hitTime);
						} else {
							
							subscription.submit(cot, hitTime);
						}
					}

				} catch (Exception e) {
					logger.warn("Error submitting event to subscription: " + subscription == null ? "null" : subscription.clientUid + " " + subscription == null ? "null" : subscription.callsign);

					if (subscription != null) {
						try {
							subMgr.deleteSubscription(subscription.uid);
						} catch (Exception e1) {
							logger.error("Exception cleaning up subscription: " + e.getMessage());
						}
					}
				}
			}

			if (!websocketHits.isEmpty()) {
				WebsocketMessagingBroker.brokerWebSocketMessage(websocketHits, cot,
						DistributedConfiguration.getInstance().getNetwork().getServerId());
			}

			// clear the sub list from the cot message - so it can be garbage-collected
			cot.setContextValue(Constants.SUBSCRIBER_HITS_KEY, null);

		} catch (Exception e) {
			logger.error("exception in broker service processMessage", e);
		}	
	}

	// TODO: something smart
	protected static int findPriority(CotEventContainer c) {
		return 0;
	}

	@Override
	public boolean hasRoomInQueueFor(CotEventContainer c) {
		// int importance = findPriority(c);
		return inputQueue.getQueueMetrics().currentSize.get() < inputQueue.getQueueMetrics().capacity.get();
	}

	public QueueMetric getQueueMetrics() {
		return inputQueue.getQueueMetrics();
	}
}