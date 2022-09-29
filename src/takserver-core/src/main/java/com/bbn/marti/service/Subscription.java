

package com.bbn.marti.service;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.cot.filter.DropEventFilter;
import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.server.NioServer;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.Tuple;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessageConversionUtil.CotEndpoint;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.collect.Lists;

import tak.server.Constants;
import tak.server.cluster.ClusterManager;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.cache.IgniteCacheHolder;

public class Subscription extends RemoteSubscription {
	
	private static final Logger log = LoggerFactory.getLogger(Subscription.class);
	
	protected final Queue<CotEventContainer> messageWriteQueueSize = new CircularFifoQueue<>(DistributedConfiguration.getInstance().getBuffer().getQueue().getMessageWriteQueueSize());

	protected final AtomicBoolean writeComplete = new AtomicBoolean(true);

	private static final String className = Subscription.class.getSimpleName();

	protected final Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();

	public static String getClassName() {
		return className;
	}

	protected static EnumSet<TransportCotEvent> subscriptionTransports = EnumSet.of(TransportCotEvent.TCP, TransportCotEvent.UDP, TransportCotEvent.STCP);

	private static final long serialVersionUID = -4032164972566035627L;

	List<DropEventFilter> dropFilters = null;
	public GeospatialEventFilter geospatialEventFilter = null;

	// keep a fresh SA message for this subscription so that it can be disseminated when required
	protected CotEventContainer latestSA;

	public CotEventContainer getLatestSA() {
		return latestSA;
	}

	public void setLatestSA(CotEventContainer latestSA) {
		// don't store messages that came from a data feed as latest SA
        if (isDataFeed.get()) return;
        
		this.latestSA = latestSA;
		
		String prevTeam = this.team;
		String prevRole = this.role;
		String prevTakv = this.takv;
		
		try {
			this.team = latestSA.getDocument().selectSingleNode("/event/detail/__group/@name").getText();
			this.role = latestSA.getDocument().selectSingleNode("/event/detail/__group/@role").getText();
			this.takv = latestSA.getDocument().selectSingleNode("/event/detail/takv/@platform").getText() + ":" +
					latestSA.getDocument().selectSingleNode("/event/detail/takv/@version").getText();
		} catch (Exception e) {
			this.team = "unknown";
			this.role = "unknown";
			this.takv = "unknown";
		}
		
		if (!this.team.equals(prevTeam) || !this.role.equals(prevRole) || !this.takv.equals(prevTakv)) {
			RemoteSubscription rs = new RemoteSubscription(this);
			rs.prepareForSerialization();

			IgniteCacheHolder.getIgniteSubscriptionClientUidTackerCache().put(clientUid, rs);
			IgniteCacheHolder.getIgniteSubscriptionUidTackerCache().put(uid, rs);
		}
	}

	protected ChannelHandler handler;

	public ChannelHandler getHandler() {
		return this.handler;
	}

	Protocol<CotEventContainer> encoder;

	public Protocol<CotEventContainer> getProtocol() {
		return encoder;
	}

	public Protocol<CotEventContainer> getEncoder() {
		return encoder;
	}

	public void setEncoder(Protocol<CotEventContainer> encoder) {
		this.encoder = encoder;
	}

	public void setHandler(ChannelHandler handler) {
		this.handler = handler;
	}

	public Subscription(RemoteSubscription sub, int ttl) throws IOException {
		this(sub.to, ttl);
	}

	public Subscription(String endpoint, int ttl) throws IOException {
		this(MessageConversionUtil.parseCotEndpoint(endpoint), ttl);
	}

	public Subscription(CotEndpoint endpoint, int ttl) throws IOException {
		if(endpoint == null) {
			throw new IOException("Error parsing CoT endpoint: " + endpoint);
		}

		this.originNode = ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()).node().id();
		// only want stcp/tcp/udp here
		Assertion.condition(subscriptionTransports.contains(endpoint.transport()));

		Tuple<ChannelHandler,Protocol<CotEventContainer>> handlerAndProtocol = endpoint.transport().client(
				endpoint.address(),
				endpoint.port(),
				SpringContextBeanForApi.getSpringContext().getBean(NioServer.class),
				Lists.newArrayList(Codec.defaultCodecSource)
				);

		ChannelHandler handler = handlerAndProtocol.left();
		Protocol<CotEventContainer> cotProtocol = handlerAndProtocol.right();

		this.handler = handler;
		this.encoder = cotProtocol;
	}

	public Subscription() {
		this.originNode = ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()).node().id();
	}

	public void submit(CotEventContainer message) throws Exception {

		this.submit(message, System.currentTimeMillis());
	}
	
	public void submit(CotEventContainer message, long hitTime) throws Exception {
		
		// for benchmarking only - disable message dissemination to clients
		if (!config.getDissemination().isEnabled()) {
			return;
		}
		
		totalSubmitted.incrementAndGet();

		// increment the hit time in the super class
		super.incHit(hitTime);
		
		if (config.getDissemination().isBoundedSubscriptionWrite()) {
			messageWriteQueueSize.add(message);
		}

		if (!config.getDissemination().isBoundedSubscriptionWrite()) { // submit message for sending directly in this thread
			
			doSubmit(message);
			
		} else { // submit message for sending in messageWritePool thread
			
			if (writeComplete.compareAndSet(true, false)) {

				Resources.messageWritePoolExecutor.execute(() -> {

					try {

						CotEventContainer lambdaMessage = messageWriteQueueSize.poll();

						if (lambdaMessage == null) {
							writeComplete.set(true);
							return;
						}

						doSubmit(lambdaMessage);

					} catch (Exception e) {
						log.warn("exception enqueuing subscription write", e);
					}
				});
			}
		}
		
		try {
			ClusterManager.countMessageSent();
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("exception tracking clustered message sent count", e);
			}
		}
	};

	private void doSubmit(CotEventContainer message) throws Exception {
		try {
			AsyncFuture<Integer> rval = this.encoder.write(message, this.handler);
			// certain exceptions happen before the future even gets scheduled
			if (rval != null && rval.getStatus() == AsyncFuture.Outcome.EXCEPT) {

				if (config.getBuffer().getQueue().isDisconnectOnFull()) {

					if (log.isDebugEnabled()) {
						log.debug("Exception in write to subscription " + this.uid, rval.getException());
					} else if (log.isWarnEnabled()) {
						log.warn("Write to subscription " + this.uid + " failed, cleaning up", rval.getException());
					}
					throw rval.getException();

				} else {
					if (log.isDebugEnabled()) {
						log.debug("Exception in doSubmit " + this.uid, rval.getException());
					} 
				}
			}
		} catch (Exception e) {
			if(message.getType().compareTo("t-x-d-d") == 0) {
				if (log.isTraceEnabled()) {
					log.trace("Write disconnect to subscription " + this.uid + " failed, cleaning up");
				}
			} else {
				if (log.isWarnEnabled()) {
					log.warn("Write to subscription " + this.uid + " failed, cleaning up", e);
				}
			}
		} finally {
			writeComplete.set(true);
		}
	}

	public String toString() {
		return "Subscription -- \nhandler: " + (this.handler == null ? "" : this.handler.toString()) + " \nxpath: " + xpath + " user: " + getUser();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((handler == null) ? 0 : handler.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (handler == null) {
			if (other.handler != null)
				return false;
		} else if (!handler.equals(other.handler))
			return false;
		return true;
	}
	
	public static RemoteSubscription copyAsRemoteSubscription(Subscription original) {
		
		if (original == null) {
			return null;
		}
		
		RemoteSubscription result = new RemoteSubscription();
		
		result.uid = original.uid;
		result.to = original.to;
		result.xpath = original.xpath;
		result.isFederated.set(original.isFederated.get());
		result.suspended = original.suspended;
		result.numHits = original.numHits;
		result.writeQueueDepth = original.writeQueueDepth;
		result.lastProcTime = original.lastProcTime;
		result.notes = original.notes;
		result.callsign = original.callsign;
		result.clientUid = original.clientUid;
		result.team = original.team;
		result.role = original.role;
		result.takv = original.takv;
		result.incognito = original.incognito;
        result.filterGroups = original.filterGroups;
        result.handlerType = original.handlerType;
        result.setUser(original.getUser());

        result.mode = original.mode;
        result.currentBandwidth = original.currentBandwidth;
        result.currentQueueDepth = original.currentQueueDepth;
        
        return result;
    }
}
