package tak.server.federation;

import java.util.Locale;
import java.util.NavigableSet;
import java.util.Set;

import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.SubscriptionStore;

import io.micrometer.core.instrument.Metrics;

import static java.util.Objects.requireNonNull;

/*
 * 
 * A subclass of FederateSubscription to track handle fed subscriptions coming into the v2 federation server
 * 
 */
public class FigServerFederateSubscription extends FigFederateSubscription {

	private static final long serialVersionUID = 5628638919041328040L;

	private static final Logger logger = LoggerFactory.getLogger(FigServerFederateSubscription.class);
	
	private GuardedStreamHolder<FederatedEvent> clientEventStreamHolder;
	private GuardedStreamHolder<ROL> clientROLEventStreamHolder;

	private String federateId;

	private final String sessionId;
	
	public FigServerFederateSubscription(String sessionId, Federate federate) {
		super(null);
		
		if (sessionId == null || sessionId.isEmpty()) {
			throw new IllegalArgumentException("empty sessionId provided for FigServerFederateSubscription creation");
		}
		
		this.sessionId = sessionId;
		this.federateId = federate.getId();
	}

	@Override
	public void submitLocalContact(FederatedEvent e, long hitTime) {
		if (localContactUid.contains(e.getContact().getUid())) {
			if (logger.isDebugEnabled()) {
				logger.debug("skipping sending contact: " + e.getContact().getCallsign() + ":" + e.getContact().getOperation());
			}
			return;
		} else {
			
			localContactUid.add(e.getContact().getUid());

			try {
				Metrics.counter(Constants.METRIC_FED_CONTACT_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("error recording fed message write metric", ex);
				}
			}
			
			try {
				if (lazyGetClientStream() != null) {
					lazyGetClientStream().send(e); // send the contact message to v2 fed client
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception sending v2 fed message", ex);
				}
			}
		}
	}

	@Override
	public void submit(final CotEventContainer toSend, long hitTime) {
		
		totalSubmitted.incrementAndGet();
		
        ((AbstractBroadcastingChannelHandler) getHandler()).getConnectionInfo().getProcessedCount().getAndIncrement();
        
		if (logger.isDebugEnabled()) {
			logger.debug("v2 fed subsciption submit message: " + toSend);
		}

		if (toSend.getContextValue(Constants.NOFEDV2_KEY) != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Explicitly not sending forwarding FIG message back to FIG");
			}
			return;
		}

		if (!isVoidWarranty() && toSend.hasContextKey(GroupFederationUtil.FEDERATE_ID_KEY)) {
			if (logger.isDebugEnabled()) {
				logger.debug("not federating federated message");
			}
			return;
		}
		
		if (toSend.getContextValue(Constants.DATA_FEED_KEY) != null && !DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
			if (logger.isDebugEnabled()) {
				logger.debug("data feed federation disabled");
			}
			return;
		}

		// increment the hit count in the super class
		super.incHit(hitTime);

		Set<String> outGroups = null;
		// Filter the incoming remote groups based on the federate's outbound groups
		NavigableSet<Group> groups = (NavigableSet<Group>) toSend.getContext(Constants.GROUPS_KEY);

		if (getFederate(federateId).isFederatedGroupMapping()) {
			if (groups != null) {
				Federate federate = getFederate(federateId);
				if (federate != null) {
					NavigableSet<Group> inGroups = GroupFederationUtil.getInstance().filterGroupDirection(Direction.IN, groups);
					outGroups = GroupFederationUtil.getInstance().filterFedOutboundGroups(federate.getOutboundGroup(), inGroups, federate.getId());
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("unable to add federate groups to Fed Event, federate is null ");
					}
				}
			}
		}

		if (toSend.getType().equals("b-f-t-r")) {
			if (toSend.getContextValue(Constants.NOFEDV2_KEY) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Explicitly ignoring FIG mission package announcement");
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("mission package announce for v2 federation server: " + toSend.asXml());
				}
				
				ROL announceROL = GroupFederationUtil.getInstance().generatePackageAnnounceROL(toSend);

				if (outGroups != null) {
					ROL.Builder builder = announceROL.toBuilder();
					builder.addAllFederateGroups(outGroups);
					announceROL = builder.build();
				}

				if (logger.isDebugEnabled()) {
					logger.debug("mp announce ROL: " + announceROL.getProgram());
				}
				
				requireNonNull(lazyGetROLClientStream(), "client ROL stream").send(announceROL);
			}
		} else if (toSend.getType().compareTo("t-x-d-d") == 0) {
			removeLocalContact(toSend.getUid());

			try {
				FederatedEvent federatedEvent = ProtoBufHelper.getInstance().delContact2protoBuf(toSend);

				if (outGroups != null) {
					FederatedEvent.Builder builder = federatedEvent.toBuilder();
					builder.addAllFederateGroups(outGroups);
					federatedEvent = builder.build();
				}

				requireNonNull(lazyGetClientStream(), "client stream").send(federatedEvent); // convert CoT message to FederatedEvent, and enqueue to send to the FIG server, respecting flow control
			} catch (Exception e) {
				logger.debug("exception sending v2 fed message", e);
			}
		} else if (toSend.getType().toLowerCase(Locale.ENGLISH).startsWith("t-x-m")) {

			if (logger.isDebugEnabled()) {
				logger.debug("not federating mission change " + toSend.asXml());
			}

		} else {
			try {
				try {
					// Filter the incoming remote groups based on the federate's outbound groups
					FederatedEvent.Builder builder = FederatedEvent.newBuilder();

					if (outGroups != null) {
						builder.addAllFederateGroups(outGroups);
					}

					FederatedEvent fEvent = builder.setEvent(ProtoBufHelper.getInstance().cot2protoBuf(toSend)).build();
					if (logger.isDebugEnabled()) {
						logger.debug("sending FederatedEvent: " + fEvent);
					}
					requireNonNull(lazyGetClientStream(), "client stream").send(fEvent);
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception sending v2 fed message", e);
					}
				}

			} catch (NumberFormatException e) {
				// ignore people putting NaN as their elevation
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception sending v2 client message", e);
				}
			}
		} 
	}
	
	

	@Override
	public void submitFederateGroups(Set<String> federateGroups) {
		try {
			SubscriptionStore.getInstance().getServerGroupStreamBySession(sessionId)
				.onNext(FederateGroups.newBuilder().addAllFederateGroups(federateGroups).build());
		} catch (Exception e) {
			logger.error("Error submitting server federate groups");
		}
	}

	@Override
    public String toString() {
        return "FigServerFederateSubscription [" + super.toString() + "]";
    }
	
	private GuardedStreamHolder<FederatedEvent> lazyGetClientStream() {
		if (clientEventStreamHolder == null) {
			clientEventStreamHolder = SubscriptionStore.getInstanceFederatedSubscriptionManager().getClientStreamBySession(sessionId);
		}
		
		return clientEventStreamHolder;
	}
	
	public GuardedStreamHolder<ROL> lazyGetROLClientStream() {
		if (clientROLEventStreamHolder == null) {
			clientROLEventStreamHolder = SubscriptionStore.getInstanceFederatedSubscriptionManager().getClientROLStreamBySession(sessionId);
		}
		
		return clientROLEventStreamHolder;
	}
	
	@Override
	public Federate getFederate() {
        return DistributedFederationManager.getInstance().getFederate(federateId);
    }
}
