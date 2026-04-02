package tak.server.federation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateGroupHopLimit;
import com.atakmap.Tak.FederateGroupHopLimits;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateHops;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity.ConnectionType;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.Subscription;

import io.grpc.ClientCall;
import io.grpc.stub.StreamObserver;

/*
 *
 * Wrapper class guarding calls to send events into stream. The instance of this class acts as the guard.
 *
 */
public class FedhubGuardedStreamHolder<T> extends GuardedStreamHolder<T>{
	private static final Logger logger = LoggerFactory.getLogger(FedhubGuardedStreamHolder.class);
    
	private static final FederateHops DEFAULT_HOPS = FederateHops.newBuilder().setCurrentHops(2).setMaxHops(-1).build();
	
    private String clientFingerprint;
    private List<String> clientGroups;
    
    private final Set<T> cache;

    public Set<T> getCache() {
        return cache;
    }
    
    // for outgoing connections
    public FedhubGuardedStreamHolder(ClientCall<T, Subscription> clientCall, String fedId, String clientFingerprint, 
    		List<String> clientGroups, FederateProvenance federateProvenance, Comparator<T> comp) {
    	super(clientCall, fedId, federateProvenance);
    	
    	this.clientFingerprint = clientFingerprint;
    	this.clientGroups = clientGroups;
    	this.federateProvenance = federateProvenance;
    	this.cache = new ConcurrentSkipListSet<T>(comp);
    }

    // for incoming connections
    public FedhubGuardedStreamHolder(StreamObserver<T> clientStream, String clientName, String certHash,
    		String sessionId, Subscription subscription, String clientFingerprint, List<String> clientGroups, 
    		FederateProvenance federateProvenance, Comparator<T> comp) {
    	super(clientStream, clientName, certHash, sessionId, subscription, federateProvenance);
    	
    	this.clientFingerprint = clientFingerprint;
    	this.clientGroups = clientGroups;
    	this.federateProvenance = federateProvenance;
    	this.cache = new ConcurrentSkipListSet<T>(comp);
    }
    
    @Override
    public synchronized void send(T event) {
        if (event == null) {
            return;
        }
        
        T modifiedEvent = null;
        
        modifiedEvent = addPropertiesToEvent(event);
        
        // possible reasons for null: message is at its hop limit
		if (modifiedEvent == null) {
			return;
		}

		// clientStream = stream of messages going from server to a connected outgoing client
		if (clientStream != null) {
			if (isWritable(modifiedEvent)) {
				clientStream.onNext(modifiedEvent);
			} else {
				totalMessagesDropped.getAndIncrement();
			}
		} 

		// clientCall = stream of messages going from outgoing client to a server
		if (clientCall != null) {
			if (isWritable(modifiedEvent)) {
				clientCall.sendMessage(modifiedEvent);
			} else {
				totalMessagesDropped.getAndIncrement();
			}
		}
    }
    
    @Override
    public T addPropertiesToEvent(T event) {
    	Set<FederateProvenance> federateProvenances = new HashSet<>();
    	federateProvenances.add(federateProvenance);
    	
        if (event instanceof FederatedEvent) {
        	FederatedEvent fedEvent = (FederatedEvent) event;
        	FederatedEvent.Builder builder = fedEvent.toBuilder();
        	
        	// if individual group hop limits are enabled, use those over global federate hop limit
			if (fedEvent.hasFederateGroupHopLimits() && fedEvent.getFederateGroupHopLimits().getUseFederateGroupHopLimits()) {
				// if federate group hops exist, check/increment them. return null if no hops left
				FederateGroupHopLimits federateGroupHopLimits = checkGroupHops(fedEvent.getFederateGroupHopLimits());

				if (federateGroupHopLimits == null) {
					return null;
				} else {
					builder.setFederateGroupHopLimits(federateGroupHopLimits);
					// still set and increment the global hops for tracking purposes
					FederateHops hops = fedEvent.getFederateHops().toBuilder()
							.setCurrentHops(fedEvent.getFederateHops().getCurrentHops() + 1).build();
					builder.setFederateHops(hops);
				}
			} else {
				// if hops exist, check/increment them. if no hops exist, set unlimited hops
				FederateHops federateHops = fedEvent.hasFederateHops() ? checkHops(event, fedEvent.getFederateHops()) : DEFAULT_HOPS;

				if (federateHops == null) {
					return null;
				} else {
					builder.setFederateHops(federateHops);
				}
			}

        	federateProvenances.addAll(fedEvent.getFederateProvenanceList());
    		builder.clearFederateProvenance();
    		builder.addAllFederateProvenance(federateProvenances);
        	
        	return (T) builder.build();
        }
        
        if (event instanceof FederateGroups) {
        	FederateGroups fedGroup = (FederateGroups) event;
        	FederateGroups.Builder builder = fedGroup.toBuilder();
        	
        	// group case is more complex and the hops are checked before
        	// arriving here. therefore if we have groups at this stage they are
        	// allowed to be sent. otherwise return null as usual to signal
        	// the message should be dropped
        	if (builder.getFederateGroupsList().size() > 0) {
        		federateProvenances.addAll(fedGroup.getFederateProvenanceList());
        		builder.clearFederateProvenance();
        		builder.addAllFederateProvenance(federateProvenances);
        	} else {
        		return null;
        	}
        	
        	return (T) builder.build();
        }
        
        if (event instanceof ROL) {
        	ROL rol = (ROL) event;
        	ROL.Builder builder = rol.toBuilder();
        	
        	// if individual group hop limits are enabled, use those over global federate hop limit
			if (rol.hasFederateGroupHopLimits() && rol.getFederateGroupHopLimits().getUseFederateGroupHopLimits()) {
				// if federate group hops exist, check/increment them. return null if no hops left
				FederateGroupHopLimits federateGroupHopLimits = checkGroupHops(rol.getFederateGroupHopLimits());

				if (federateGroupHopLimits == null) {
					return null;
				} else {
					builder.setFederateGroupHopLimits(federateGroupHopLimits);
					// still set and increment the global hops for tracking purposes
					FederateHops hops = rol.getFederateHops().toBuilder()
							.setCurrentHops(rol.getFederateHops().getCurrentHops() + 1).build();
					builder.setFederateHops(hops);
				}
			} else {
				// if hops exist, check/increment them. if no hops exist, set unlimited hops
				FederateHops federateHops = rol.hasFederateHops() ? checkHops(event, rol.getFederateHops()) : DEFAULT_HOPS;

				if (federateHops == null) {
					return null;
				} else {
					builder.setFederateHops(federateHops);
				}
			}
        	
        	federateProvenances.addAll(rol.getFederateProvenanceList());
    		builder.clearFederateProvenance();
    		builder.addAllFederateProvenance(federateProvenances);
        	
        	return (T) builder.build();
        }
        
        if (event instanceof BinaryBlob) {
        	BinaryBlob blob = (BinaryBlob) event;
        	BinaryBlob.Builder builder = blob.toBuilder();
        	
        	// if individual group hop limits are enabled, use those over global federate hop limit
			if (blob.hasFederateGroupHopLimits() && blob.getFederateGroupHopLimits().getUseFederateGroupHopLimits()) {
				// if federate group hops exist, check/increment them. return null if no hops left
				FederateGroupHopLimits federateGroupHopLimits = checkGroupHops(blob.getFederateGroupHopLimits());

				if (federateGroupHopLimits == null) {
					return null;
				} else {
					builder.setFederateGroupHopLimits(federateGroupHopLimits);
					// still set and increment the global hops for tracking purposes
					FederateHops hops = blob.getFederateHops().toBuilder()
							.setCurrentHops(blob.getFederateHops().getCurrentHops() + 1).build();
					builder.setFederateHops(hops);
				}
			} else {
				// if hops exist, check/increment them. if no hops exist, set unlimited hops
				FederateHops federateHops = blob.hasFederateHops() ? checkHops(event, blob.getFederateHops()) : DEFAULT_HOPS;

				if (federateHops == null) {
					return null;
				} else {
					builder.setFederateHops(federateHops);
				}
			}
        	
        	federateProvenances.addAll(blob.getFederateProvenanceList());
    		builder.clearFederateProvenance();
    		builder.addAllFederateProvenance(federateProvenances);
        	
        	return (T) builder.build();
        }
        
		return null;
    }
    
    private FederateGroupHopLimits checkGroupHops(FederateGroupHopLimits federateGroupHopLimits) {
    	List<FederateGroupHopLimit> updatedLimits = federateGroupHopLimits.getLimitsList().stream().map(limit -> {
			return limit.toBuilder().setCurrentHops(limit.getCurrentHops() + 1).build();
		}).collect(Collectors.toList());
    	
    	boolean anyHopsLeft = updatedLimits.stream().anyMatch(limit -> {
    		long maxHops = limit.getMaxHops();
    		long currentHops = limit.getCurrentHops();
    		
    		if (currentHops > maxHops && maxHops != -1) {
    			return false;
    		} 
    		// if there is only 1 hop left and the next hop is to a federation hub,
    		// just drop the message now because the next hub will not be allowed to send it
    		// further anyways
    		else if (isHubSubscription() && currentHops == maxHops) {
    			return false;
    		} else {
    			return true;
    		}
    	});
    	
    	return anyHopsLeft ? federateGroupHopLimits.toBuilder().clearLimits().addAllLimits(updatedLimits).build() : null;
    }
    
    private FederateHops checkHops(T event, FederateHops federateHops) {
    	long maxHops = federateHops.getMaxHops();
		long currentHops = federateHops.getCurrentHops() + 1;
		
		if (currentHops > maxHops && maxHops != -1) {
			if (logger.isDebugEnabled()) {
				logger.debug("dropping message because of hop limit " + event);
			}
			return null;
		} 
		// if there is only 1 hop left and the next hop is to a federation hub,
		// just drop the message now because the next hub will not be allowed to send it
		// further anyways
		else if (isHubSubscription() && currentHops == maxHops) {  
			if (logger.isDebugEnabled()) {
				logger.debug("dropping message because of hop limit (1 hop left but next destination is a hub) " + event);
			}
			return null;
		} else {
			return FederateHops.newBuilder().setCurrentHops(currentHops).setMaxHops(maxHops).build();
		}
    }

    // if the subscription associated with this connection is a federation hub
    private boolean isHubSubscription() {
    	return subscription != null && 
    			(subscription.getIdentity().getType() == ConnectionType.FEDERATION_HUB_CLIENT 
    			|| subscription.getIdentity().getType() == ConnectionType.FEDERATION_HUB_SERVER);
    }
    
    public void setMaxFederateHops(int maxFederateHops) {
    	this.maxFederateHops = maxFederateHops;
    }

	public String getClientFingerprint() {
		return clientFingerprint;
	}

	public List<String> getClientGroups() {
		return clientGroups;
	}
}
