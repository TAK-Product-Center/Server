package tak.server.federation;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.net.ssl.SSLSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederatedEvent;
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
    
    private boolean isHub = false;

    private final Set<T> cache;

    public Set<T> getCache() {
        return cache;
    }
    
    // for outgoing connections
    public GuardedStreamHolder(ClientCall<T, Subscription> clientCall, String fedId, Comparator<T> comp, boolean isHub) {

        requireNonNull(clientCall, "FederatedEvent groupCall");

        requireNonNull(comp, "comparator");
        
        this.isHub = isHub;

        this.cache = new ConcurrentSkipListSet<T>(comp);
        
        this.federateIdentity = new FederateIdentity(fedId);

        this.clientCall = clientCall;
        
        lastHealthTime = System.currentTimeMillis();
        lastHealthStatus = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();
    }

    // for incoming connections
    public GuardedStreamHolder(StreamObserver<T> clientStream, String clientName, String certHash, SSLSession session, Subscription subscription, Comparator<T> comp, boolean isHub) {

        requireNonNull(clientStream, "FederatedEvent client stream");

        requireNonNull(subscription, "client subscription");
        requireNonNull(comp, "comparator");
        
        this.isHub = isHub;
        
        this.cache = new ConcurrentSkipListSet<T>(comp);

        if (Strings.isNullOrEmpty(clientName)) {
            throw new IllegalArgumentException("empty client name - invalid stream");
        }

        if (Strings.isNullOrEmpty(certHash)) {
            throw new IllegalArgumentException("empty cert hash - invalid stream");
        }

        // append a random id to the end, to prevent collisions. this is done for outgoing connections as well in the javascript code
        String fedId = clientName + "-" + certHash  + "-" + new BigInteger(session.getId());

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
        
        if (isHub) {
        	// since hub outgoing connections can forward traffic to other hubs, we need to keep a list of visited nodes
            // so that we can stop cycles
            FederateProvenance prov = FederateProvenance.newBuilder()
        			.setFederationServerId(FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfig().getFullId())
        			.setFederationServerName(FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfig().getServerName())
        			.build();
            
            if (event instanceof FederatedEvent) {
            	FederatedEvent fedEvent = (FederatedEvent) event;
            	List<FederateProvenance> federateProvenances = new ArrayList<>(fedEvent.getFederateProvenanceList());
            	federateProvenances.add(prov);
            	
            	event = (T) fedEvent.toBuilder().addAllFederateProvenance(federateProvenances).build();
            }
            
            if (event instanceof FederateGroups) {
            	FederateGroups fedGroup = (FederateGroups) event;
            	List<FederateProvenance> federateProvenances = new ArrayList<>(fedGroup.getFederateProvenanceList());
            	federateProvenances.add(prov);
            	
            	event = (T) fedGroup.toBuilder().addAllFederateProvenance(federateProvenances).build();
            }
            
            if (event instanceof ROL) {
            	ROL rol = (ROL) event;
            	List<FederateProvenance> federateProvenances = new ArrayList<>(rol.getFederateProvenanceList());
            	federateProvenances.add(prov);
            	
            	event = (T) rol.toBuilder().addAllFederateProvenance(federateProvenances).build();
            }
            
            if (event instanceof BinaryBlob) {
            	BinaryBlob blob = (BinaryBlob) event;
            	List<FederateProvenance> federateProvenances = new ArrayList<>(blob.getFederateProvenanceList());
            	federateProvenances.add(prov);
            	
            	event = (T) blob.toBuilder().addAllFederateProvenance(federateProvenances).build();
            }
        }
        
        // clientStream = stream of messages going from server to a connected outgoing client
        if (clientStream != null) 
        	clientStream.onNext(event);
        
        // clientCall = stream of messages going from outgoing client to a server
        if (clientCall != null)  
        	clientCall.sendMessage(event);
    }

    public void throwDeadlineExceptionToClient() {
        try {
        	if (clientStream != null)
        		clientStream.onError(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));
        } catch (Exception e) {
            logger.warn("exception sending StatusRuntimeException - DEADLINE_EXCEEDED to client", e);
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

    public FederateIdentity getFederateIdentity() {
        return federateIdentity;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    @Override
	public String toString() {
		return "GuardedStreamHolder [clientStream=" + clientStream + ", clientCall=" + clientCall + ", lastHealthTime="
				+ lastHealthTime + ", lastHealthStatus=" + lastHealthStatus + ", federateIdentity=" + federateIdentity
				+ ", subscription=" + subscription + ", cache=" + cache + "]";
	}
}
