package tak.server.federation;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.Subscription;
import com.google.common.base.Strings;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/*
 *
 * Wrapper class guarding calls to send events into stream. The instance of this class acts as the guard.
 *
 */
public class GuardedStreamHolder<T> {

    private static final Logger logger = LoggerFactory.getLogger(GuardedStreamHolder.class);

    private final StreamObserver<T> clientStream;
    private long lastHealthTime;
    private ClientHealth lastHealthStatus;
    private final FederateIdentity federateIdentity;
    private final Subscription subscription;

    private final Set<T> cache;

    public Set<T> getCache() {
        return cache;
    }

    public GuardedStreamHolder(StreamObserver<T> clientStream, String clientName, String certHash, Subscription subscription, Comparator<T> comp) {

        requireNonNull(clientStream, "FederatedEvent client stream");

        requireNonNull(subscription, "client subscription");
        requireNonNull(comp, "comparator");

        this.cache = new ConcurrentSkipListSet<T>(comp);

        if (Strings.isNullOrEmpty(clientName)) {
            throw new IllegalArgumentException("empty client name - invalid stream");
        }

        if (Strings.isNullOrEmpty(certHash)) {
            throw new IllegalArgumentException("empty cert hash - invalid stream");
        }

        String fedId = clientName + "-" + certHash;

        this.subscription = subscription;

        this.federateIdentity = new FederateIdentity(fedId);

        this.clientStream = clientStream;
        lastHealthTime = System.currentTimeMillis();
        lastHealthStatus = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();
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

        clientStream.onNext(event);
    }

    public void throwDeadlineExceptionToClient() {
        try {
            clientStream.onError(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));
        } catch (Exception e) {
            logger.warn("exception sending StatusRuntimeException - DEADLINE_EXCEEDED to client", e);
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
        StringBuilder builder = new StringBuilder();
        builder.append("GuardedStreamHolder [clientStream=");
        builder.append(clientStream);
        builder.append(", timeSinceLastHealthCheck=");
        builder.append(lastHealthTime);
        builder.append(", lastHealthStatus=");
        builder.append(lastHealthStatus);
        builder.append(", federateIdentity=");
        builder.append(federateIdentity);
        builder.append(", subscription=");
        builder.append(subscription);
        builder.append("]");
        return builder.toString();
    }
}
