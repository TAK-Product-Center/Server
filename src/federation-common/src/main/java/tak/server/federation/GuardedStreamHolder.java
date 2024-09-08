package tak.server.federation;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateHops;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity.ConnectionType;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.Subscription;
import com.google.common.base.Strings;

import io.grpc.ClientCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;

/*
 *
 * Wrapper class guarding calls to send events into stream. The instance of this class acts as the guard.
 *
 */
public class GuardedStreamHolder<T> {

    private static final Logger logger = LoggerFactory.getLogger(GuardedStreamHolder.class);

    private StreamObserver<T> clientStream;
    private ClientCall<T, Subscription> clientCall;
    private long lastHealthTime;
    private ClientHealth lastHealthStatus;
    private FederateIdentity federateIdentity;
    private Subscription subscription;
    private int maxFederateHops = -1;
    private String clientFingerprint;
    private List<String> clientGroups;
    
    private boolean isRunningInHub = false;

    private final Set<T> cache;

    public Set<T> getCache() {
        return cache;
    }
    
    // for outgoing connections
    public GuardedStreamHolder(ClientCall<T, Subscription> clientCall, String fedId, Comparator<T> comp, boolean isRunningInHub) {

    	requireNonNull(clientCall, "FederatedEvent groupCall");

        requireNonNull(comp, "comparator");
        
        this.isRunningInHub = isRunningInHub;

        this.cache = new ConcurrentSkipListSet<T>(comp);
        
        this.federateIdentity = new FederateIdentity(fedId);

        this.clientCall = clientCall;
        
        lastHealthTime = System.currentTimeMillis();
        lastHealthStatus = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();
    }

    // for incoming connections
    public GuardedStreamHolder(StreamObserver<T> clientStream, String clientName, String certHash, String sessionId, Subscription subscription, Comparator<T> comp, boolean isRunningInHub) {

        requireNonNull(clientStream, "FederatedEvent client stream");

        requireNonNull(subscription, "client subscription");
        requireNonNull(comp, "comparator");
        
        this.isRunningInHub = isRunningInHub;
        
        this.cache = new ConcurrentSkipListSet<T>(comp);

        if (Strings.isNullOrEmpty(clientName)) {
            throw new IllegalArgumentException("empty client name - invalid stream");
        }

        if (Strings.isNullOrEmpty(certHash)) {
            throw new IllegalArgumentException("empty cert hash - invalid stream");
        }

        // new takservers will send their CoreConfig serverId. if present, use it, otherwise generate a random unique identifier
        String serverId = subscription.getIdentity().getServerId();
        if (Strings.isNullOrEmpty(serverId)) {
        	serverId = sessionId;
        }
        String fedId = clientName + "-" + certHash  + "-" + serverId;

        this.subscription = subscription;

        this.federateIdentity = new FederateIdentity(fedId);

        this.clientStream = clientStream;
        lastHealthTime = System.currentTimeMillis();
        lastHealthStatus = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();
    }
    
    public void setSubscription(Subscription sub) {
    	this.subscription = sub;
    }

    public void updateClientHealth(ClientHealth healthCheck) {
        this.lastHealthTime = System.currentTimeMillis();
        this.lastHealthStatus = healthCheck;
    }

    public boolean isClientHealthy(long clientTimeoutTime) {
        if (lastHealthStatus.getStatus() == ClientHealth.ServingStatus.SERVING) {
            long now = System.currentTimeMillis();
            long diff = (now - lastHealthTime) / 1000;
            if (logger.isDebugEnabled()) {
                logger.debug("now: " + now + " lastHealthTime: " + lastHealthTime + " diff: " + diff + " clientTimeoutTime: " + clientTimeoutTime);
            }

            if (diff < clientTimeoutTime) {
                return true;
            }
        }
        return false;
    }

    public synchronized void send(T event) {
        if (event == null) {
            return;
        }
        
        T modifiedEvent = null;
        if (isRunningInHub) {
        	// since hub outgoing connections can forward traffic to other hubs, we need to keep a list of visited nodes
            // so that we can stop cycles
            FederateProvenance prov = FederateProvenance.newBuilder()
        			.setFederationServerId(FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfigManager().getConfig().getFullId())
        			.setFederationServerName(FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfigManager().getConfig().getServerName())
        			.build();
            
            Set<FederateProvenance> federateProvenances = new HashSet<>();
            federateProvenances.add(prov);
            
            modifiedEvent = addPropertiesToEvent(event, federateProvenances);
        } else {
        	modifiedEvent = addPropertiesToEvent(event, null);
        }
        
        // possible reasons for null: message is at its hop limit
    	if (modifiedEvent == null) {
    		return;
    	}
    	    	        
        // clientStream = stream of messages going from server to a connected outgoing client
        if (clientStream != null) 
        	clientStream.onNext(modifiedEvent);
        
        // clientCall = stream of messages going from outgoing client to a server
        if (clientCall != null)  {
        	clientCall.sendMessage(modifiedEvent);
        }
    }
    
    public T addPropertiesToEvent(T event, Set<FederateProvenance> federateProvenances) {
        if (event instanceof FederatedEvent) {
        	FederatedEvent fedEvent = (FederatedEvent) event;
        	FederatedEvent.Builder builder = fedEvent.toBuilder();
        	
        	// if hops exist, check/increment them, if no hops exist, we need to add them
        	FederateHops federateHops = fedEvent.hasFederateHops() ? checkHops(event, fedEvent.getFederateHops()) : 
        		FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
        	
        	if (federateHops == null) {
        		return null;
        	} else {
        		builder.setFederateHops(federateHops);
        	}
        	
        	if (federateProvenances != null) {
        		federateProvenances.addAll(fedEvent.getFederateProvenanceList());
        		builder.clearFederateProvenance();
        		builder.addAllFederateProvenance(federateProvenances);
        	}
        	
        	return (T) builder.build();
        }
        
        if (event instanceof FederateGroups) {
        	FederateGroups fedGroup = (FederateGroups) event;
        	FederateGroups.Builder builder = fedGroup.toBuilder();
        	
        	// if hops exist, check/increment them, if no hops exist, we need to add them
        	FederateHops federateHops = fedGroup.hasFederateHops() ? checkHops(event, fedGroup.getFederateHops()) : 
        		FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
        	
        	if (federateHops == null) {
        		return null;
        	} else {
        		builder.setFederateHops(federateHops);
        	}
        	
        	if (federateProvenances != null) {
        		federateProvenances.addAll(fedGroup.getFederateProvenanceList());
        		builder.clearFederateProvenance();
        		builder.addAllFederateProvenance(federateProvenances);
        	}
        	
        	return (T) builder.build();
        }
        
        if (event instanceof ROL) {
        	ROL rol = (ROL) event;
        	ROL.Builder builder = rol.toBuilder();
        	
        	// if hops exist, check/increment them, if no hops exist, we need to add them
        	FederateHops federateHops = rol.hasFederateHops() ? checkHops(event, rol.getFederateHops()) : 
        		FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
        	
        	if (federateHops == null) {
        		return null;
        	} else {
        		builder.setFederateHops(federateHops);
        	}
        	
        	if (federateProvenances != null) {
        		federateProvenances.addAll(rol.getFederateProvenanceList());
        		builder.clearFederateProvenance();
        		builder.addAllFederateProvenance(federateProvenances);
        	}
        	
        	return (T) builder.build();
        }
        
        if (event instanceof BinaryBlob) {
        	BinaryBlob blob = (BinaryBlob) event;
        	BinaryBlob.Builder builder = blob.toBuilder();
        	
        	// if hops exist, check/increment them, if no hops exist, we need to add them
        	FederateHops federateHops = blob.hasFederateHops() ? checkHops(event, blob.getFederateHops()) : 
        		FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
        	
        	if (federateHops == null) {
        		return null;
        	} else {
        		builder.setFederateHops(federateHops);
        	}
        	
        	if (federateProvenances != null) {
        		federateProvenances.addAll(blob.getFederateProvenanceList());
        		builder.clearFederateProvenance();
        		builder.addAllFederateProvenance(federateProvenances);
        	}
        	
        	return (T) builder.build();
        }
        
		return null;
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
    
    public void throwDeadlineExceptionToClient() {
        try {
        	if (clientStream != null)
        		clientStream.onError(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));
        } catch (Exception e) {
            logger.warn("exception sending StatusRuntimeException - DEADLINE_EXCEEDED to client", e);
        }
    }
    
    public void throwCanceledExceptionToClient() {
        try {
        	if (clientStream != null)
        		clientStream.onError(new StatusRuntimeException(Status.CANCELLED));
        } catch (Exception e) {
            logger.warn("exception sending StatusRuntimeException - CANCELLED to client", e);
        }
    }
    
    public void throwPermissionDeniedToClient() {
        try {
        	if (clientStream != null)
        		clientStream.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
        } catch (Exception e) {
            logger.warn("exception sending StatusRuntimeException - PERMISSION_DENIED to client", e);
        }
    }
    
    public void cancel(String message, Throwable cause) {
    	if (clientCall != null) {
    		clientCall.cancel(message, cause);
    	}
    	if (clientStream != null) {
    		clientStream.onError(cause);
    	}
    }

    public FederateIdentity getFederateIdentity() {
        return federateIdentity;
    }

    public Subscription getSubscription() {
        return subscription;
    }
    
    public void setMaxFederateHops(int maxFederateHops) {
    	this.maxFederateHops = maxFederateHops;
    }

	public String getClientFingerprint() {
		return clientFingerprint;
	}

	public void setClientFingerprint(String clientFingerprint) {
		this.clientFingerprint = clientFingerprint;
	}

	public List<String> getClientGroups() {
		return clientGroups;
	}

	public void setClientGroups(List<String> clientGroups) {
		this.clientGroups = clientGroups;
	}

	@Override
	public String toString() {
		return "GuardedStreamHolder [clientStream=" + clientStream + ", clientCall=" + clientCall + ", lastHealthTime="
				+ lastHealthTime + ", lastHealthStatus=" + lastHealthStatus + ", federateIdentity=" + federateIdentity
				+ ", subscription=" + subscription + ", cache=" + cache + "]";
	}
}
