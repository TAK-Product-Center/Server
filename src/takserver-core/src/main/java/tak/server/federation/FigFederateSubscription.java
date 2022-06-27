package tak.server.federation;

import static io.grpc.MethodDescriptor.generateFullMethodName;

import java.util.Locale;
import java.util.NavigableSet;
import java.util.Set;

import com.atakmap.Tak.FederateGroups;
import io.grpc.MethodDescriptor;
import io.micrometer.core.instrument.Metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Subscription;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import tak.server.Constants;
import tak.server.cluster.ClusterManager;
import tak.server.cot.CotEventContainer;

/*
 * 
 * A subclass of FederateSubscription to handle FIG federates
 * 
 * Both types of federates share the same data types. Data is sent and received differently - by way of gRPC / Netty in the case of FIG, 
 * and with internal NIO in the case of TAK Server. 
 * 
 */
public class FigFederateSubscription extends FederateSubscription {

    private static final Logger logger = LoggerFactory.getLogger(FigFederateSubscription.class);

    private static final long serialVersionUID = 3972200398818680453L;

    private Reachability<User> reachability;

    private final TakFigClient figClient;

    private boolean isAutoMapped = false;

	private ClientCall<FederatedEvent, Subscription> clientCall;
    private ClientCall<FederateGroups, Subscription> groupsCall;
        
    public Reachability<User> getReachability() {
        return reachability;
    }

    public void setReachability(Reachability<User> reachability) {
        this.reachability = reachability;
    }
    
    @SuppressWarnings("deprecation")
	public FigFederateSubscription(TakFigClient figClient) {
        super();
        this.figClient = figClient;
        
        if (figClient != null) {
        	setupGroupStream();
        } else {
        	clientCall = null;
        	groupsCall = null;
        }

    }

    public void setLastProcTime(long curTime) {
        this.hasUpdate.set(true);
        this.lastProcTime.set(curTime);
    }

    private void setupGroupStream() {
    	groupsCall = figClient.getChannel()
				.newCall(io.grpc.MethodDescriptor.create(MethodDescriptor.MethodType.CLIENT_STREAMING,
						generateFullMethodName("com.atakmap.FederatedChannel", "ClientFederateGroupsStream"),
						io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.FederateGroups.getDefaultInstance()),
						io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance())),
						figClient.getAsyncFederatedChannel().getCallOptions());

		// use listener to respect flow control, and send messages to the server when it
		// is ready
		groupsCall.start(new ClientCall.Listener<Subscription>() {

			@Override
			public void onMessage(Subscription response) {
				// Notify gRPC to receive one additional response.
				groupsCall.request(1);
			}

			@Override
			public void onReady() {}
			
		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		groupsCall.request(1);
    }
    
    public void setupEventStream() { 
    	clientCall = figClient.getChannel().newCall(io.grpc.MethodDescriptor.create(
    			io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
    			generateFullMethodName("com.atakmap.FederatedChannel", "ServerEventStream"),
    			io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.FederatedEvent.getDefaultInstance()),
    			io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance())), figClient.getAsyncFederatedChannel().getCallOptions());

    	// use listener to respect flow control, and send messages to the server when it is ready 
    	clientCall.start(new ClientCall.Listener<Subscription>() {

    		@Override
    		public void onMessage(Subscription response) {
    			// Notify gRPC to receive one additional response.
    			clientCall.request(1);
    		}

			@Override
    		public void onReady() {

    			if (logger.isDebugEnabled()) {
    				logger.debug("ServerEventStreamclientCall ready.");
    			}
    			
    			try {
    				// send a current snapshot of the contact list (current subscriptions) to this federate
        			GroupFederationUtil.getInstance().sendLatestContactsToFederate(FigFederateSubscription.this);
    			} catch (Exception e) {
    				if (logger.isDebugEnabled()) {
    					logger.debug("exception sending latest contacts as v2 federate client", e);
    				}
    			}
    			
    			try {
    				if (config.getBuffer().getLatestSA().isEnable()) {
    					if (logger.isDebugEnabled()) {
    						logger.debug("Sending latest SA as v2 federate client");
    					}
						MessagingUtilImpl.getInstance().sendLatestReachableSA(getUser());
    				}
    			} catch (Exception e) {
    				if (logger.isDebugEnabled()) {
    					logger.debug("exception sending latest SA as v2 federate client", e);
    				}
    			}
       		}
    	}, new Metadata());
    	
    	// Notify gRPC to receive one response. Without this line, onMessage() would never be called.
        clientCall.request(1);	
    }

    private synchronized void sendMessageProtected(FederatedEvent e) {
        clientCall.sendMessage(e);
        
        try {
        	
        	if (e.hasEvent() && e.getEvent() != null) {
        		Metrics.counter(Constants.METRIC_FED_DATA_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();
        	}
        	
        	if (e.hasContact() && e.getContact() != null) {
        		Metrics.counter(Constants.METRIC_FED_CONTACT_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();
        	}
        	
        } catch (Exception ex) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("error recording fed message write metric", ex);
        	}
        }

    }

    @Override
    public void submitLocalContact(FederatedEvent e, long hitTime) {
        if(localContactUid.contains(e.getContact().getUid())) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("skipping sending contact: " + e.getContact().getCallsign() + ":" + e.getContact().getOperation());
        	}
            return;
        } else {
            localContactUid.add(e.getContact().getUid());
            super.incHit(hitTime);
            sendMessageProtected(e); // send the contact message to the FIG
        }
    }

    @Override
    public void submit(final CotEventContainer toSend, long hitTime) {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("FigFederateSubscription submit " + toSend);
    	}
    	
		// for benchmarking only - disable message dissemination to clients
		if (!config.getDissemination().isEnabled()) {
			return;
		}
    	
		totalSubmitted.incrementAndGet();
    	
        ((AbstractBroadcastingChannelHandler) getHandler()).getConnectionInfo().getProcessedCount().getAndIncrement();
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

        // increment the hit count in the super class
        super.incHit(hitTime);

        /*
        // send the message
        ImportanceSpec qosSpec = this.lqm.getImportance(toSend.getDocument());
        this.sender.addToOutputQueue(toSend, qosSpec.importance, qosSpec.replaceByPubUID);
         */

		Set<String> outGroups = null;
		if (figClient.getFederate().isFederatedGroupMapping()) {
			// Filter the incoming remote groups based on the federate's outbound groups
			NavigableSet<Group> groups = (NavigableSet<Group>) toSend.getContext(Constants.GROUPS_KEY);
			if (groups != null) {
				NavigableSet<Group> inGroups = GroupFederationUtil.getInstance().filterGroupDirection(Direction.IN, groups);
				Federate federate = getFigClient().getFederate();
				if (federate != null) {
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
            		logger.debug("mission package announce for FIG: " + toSend);
            	}
                figClient.sendPackageAnnounce(toSend, outGroups);
            }
        } else if (toSend.getType().compareTo("t-x-d-d") == 0) {
            removeLocalContact(toSend.getUid());

			FederatedEvent federatedEvent = ProtoBufHelper.getInstance().delContact2protoBuf(toSend);

			if (outGroups != null) {
				FederatedEvent.Builder builder = federatedEvent.toBuilder();
				builder.addAllFederateGroups(outGroups);
				federatedEvent = builder.build();
			}

            sendMessageProtected(federatedEvent); // convert CoT message to FederatedEvent, and send to the FIG server, respecting flow control
        } else if (toSend.getType().toLowerCase(Locale.ENGLISH).startsWith("t-x-m")) {
        	
        	if (logger.isDebugEnabled()) {
        		logger.debug("not federating mission change " + toSend.asXml());
        	}
        	
		} else {
            try {
            	FederatedEvent.Builder builder = FederatedEvent.newBuilder();

            	if (outGroups != null) {
            		builder.addAllFederateGroups(outGroups);
				}

                FederatedEvent fEvent = builder.setEvent(ProtoBufHelper.getInstance().cot2protoBuf(toSend)).build();

                if (logger.isDebugEnabled()) {
                	logger.debug("sending FederatedEvent: " + fEvent);
                }

                if (clientCall == null) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("null clientCall");
                	}
                } else {
                	sendMessageProtected(fEvent);
                }

            } catch (NumberFormatException e) {
                // ignore people putting NaN as their elevation
            } catch (IllegalArgumentException e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("invalid message", e);
            	}
            } catch (Exception e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("exception sending FederatedEvent", e);
            	}
            }
        }
        
        try {
        	ClusterManager.countMessageSent();
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking clustered message sent count", e);
			}
		}
    }
    
    public void submitFederateGroups(Set<String> federateGroups) {
        if (logger.isDebugEnabled()) {
            logger.debug("Submitting federate groups: " + federateGroups);
        }
        groupsCall.sendMessage(FederateGroups.newBuilder().addAllFederateGroups(federateGroups).build());
    }

    public void closeGroupStream(Throwable t) {
    	try {
    		groupsCall.cancel("Close group stream", t);
    	} catch (Exception e) {}
    }

    public void closeClientCall(Throwable t) {
        try {
            clientCall.cancel("Close client call", t);
        } catch (Exception e) {}
    }

    public TakFigClient getFigClient() {
        return figClient;
    }
    
    public boolean getIsAutoMapped() {
		return isAutoMapped;
	}

	public void setIsAutoMapped(boolean isAutoMapped) {
		if (isAutoMapped) logger.info("federate subscription " + this.callsign + " set to auto mapping");
		this.isAutoMapped = isAutoMapped;
	}

	public Federate getFederate() {
        return figClient.getFederate();
    }

}
