package tak.server.federation;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.FederateGroupHopLimit;
import com.atakmap.Tak.FederateGroupHopLimits;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateHops;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.Subscription;
import com.google.common.base.Strings;

import io.grpc.ClientCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;

/*
 *
 * Wrapper class guarding calls to send events into stream. The instance of this class acts as the guard.
 *
 */
public class GuardedStreamHolder<T> {
	private static final Logger logger = LoggerFactory.getLogger(GuardedStreamHolder.class);

	protected CallStreamObserver<T> clientStream;
	protected ClientCall<T, Subscription> clientCall;
	protected long lastHealthTime;
	protected ClientHealth lastHealthStatus;
	protected FederateIdentity federateIdentity;
	protected Subscription subscription;
	protected int maxFederateHops = -1;
	protected String clientFingerprint;
	protected FederateProvenance federateProvenance;

	public static AtomicLong totalMessagesDropped = new AtomicLong();

	// for outgoing connections
	public GuardedStreamHolder(ClientCall<T, Subscription> clientCall, String fedId,
			FederateProvenance federateProvenance) {

		requireNonNull(clientCall, "FederatedEvent groupCall");

		this.federateIdentity = new FederateIdentity(fedId);

		this.clientCall = clientCall;

		this.federateProvenance = federateProvenance;

		lastHealthTime = System.currentTimeMillis();
		lastHealthStatus = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();
	}

	// for incoming connections
	public GuardedStreamHolder(StreamObserver<T> clientStream, String clientName, String certHash, String sessionId,
			Subscription subscription, FederateProvenance federateProvenance) {

		requireNonNull(clientStream, "FederatedEvent client stream");

		requireNonNull(subscription, "client subscription");

		if (Strings.isNullOrEmpty(clientName)) {
			throw new IllegalArgumentException("empty client name - invalid stream");
		}

		if (Strings.isNullOrEmpty(certHash)) {
			throw new IllegalArgumentException("empty cert hash - invalid stream");
		}

		// new takservers will send their CoreConfig serverId. if present, use it,
		// otherwise generate a random unique identifier
		String serverId = subscription.getIdentity().getServerId();
		if (Strings.isNullOrEmpty(serverId)) {
			serverId = sessionId;
		}
		String fedId = clientName + "-" + certHash + "-" + serverId;

		this.subscription = subscription;

		this.federateIdentity = new FederateIdentity(fedId);

		this.clientStream = (CallStreamObserver<T>) clientStream;

		this.federateProvenance = federateProvenance;

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
				logger.debug("now: " + now + " lastHealthTime: " + lastHealthTime + " diff: " + diff
						+ " clientTimeoutTime: " + clientTimeoutTime);
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

		modifiedEvent = addPropertiesToEvent(event);

		// possible reasons for null: message is at its hop limit
		if (modifiedEvent == null) {
			return;
		}

		// clientStream = stream of messages going from server to a connected outgoing
		// client
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

	protected long getFreeMemory() {
		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		return Runtime.getRuntime().maxMemory() - usedMemory;
	}

	protected long getMaxMemeory() {
		return Runtime.getRuntime().maxMemory();
	}

	protected boolean isWritable(T event) {
		double freeMem = getFreeMemory();
		double maxMem = getMaxMemeory();

		if (event instanceof FederatedEvent) {
			// allow buffering of federated events up to 75% of the max process memory
			return (freeMem / maxMem) > 0.25;
		} else if (event instanceof FederateGroups) {
			return ((FederateGroups) event).getSerializedSize() < freeMem;
		} else if (event instanceof ROL) {
			return ((ROL) event).getSerializedSize() < freeMem;
		} else if (event instanceof BinaryBlob) {
			return ((BinaryBlob) event).getSerializedSize() < freeMem;
		} else {
			return true;
		}
	}

	public T addPropertiesToEvent(T event) {
		Set<FederateProvenance> federateProvenances = new HashSet<>();
		federateProvenances.add(federateProvenance);

		if (event instanceof FederatedEvent) {
			FederatedEvent fedEvent = (FederatedEvent) event;
			FederatedEvent.Builder builder = fedEvent.toBuilder();

			// update limits
			List<FederateGroupHopLimit> updatedLimits = fedEvent.getFederateGroupHopLimits().getLimitsList().stream()
					.map(limit -> {
						return limit.toBuilder().setCurrentHops(limit.getCurrentHops() + 1).build();
					}).collect(Collectors.toList());

			FederateGroupHopLimits limits = fedEvent.getFederateGroupHopLimits().toBuilder().clearLimits()
					.addAllLimits(updatedLimits).build();

			builder.setFederateGroupHopLimits(limits);

			// add message maxHops
			FederateHops federateHops = FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
			builder.setFederateHops(federateHops);

			federateProvenances.addAll(fedEvent.getFederateProvenanceList());
			builder.clearFederateProvenance();
			builder.addAllFederateProvenance(federateProvenances);

			return (T) builder.build();
		}

		if (event instanceof FederateGroups) {
			FederateGroups fedGroup = (FederateGroups) event;
			FederateGroups.Builder builder = fedGroup.toBuilder();

			// update limits
			List<FederateGroupHopLimit> updatedLimits = fedGroup.getFederateGroupHopLimits().getLimitsList().stream()
					.map(limit -> {
						return limit.toBuilder().setCurrentHops(limit.getCurrentHops() + 1).build();
					}).collect(Collectors.toList());

			FederateGroupHopLimits limits = fedGroup.getFederateGroupHopLimits().toBuilder().clearLimits()
					.addAllLimits(updatedLimits).build();

			builder.setFederateGroupHopLimits(limits);

			// add message maxHops
			FederateHops federateHops = FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
			builder.setFederateHops(federateHops);

			federateProvenances.addAll(fedGroup.getFederateProvenanceList());
			builder.clearFederateProvenance();
			builder.addAllFederateProvenance(federateProvenances);

			return (T) builder.build();
		}

		if (event instanceof ROL) {
			ROL rol = (ROL) event;
			ROL.Builder builder = rol.toBuilder();

			// update limits
			List<FederateGroupHopLimit> updatedLimits = rol.getFederateGroupHopLimits().getLimitsList().stream()
					.map(limit -> {
						return limit.toBuilder().setCurrentHops(limit.getCurrentHops() + 1).build();
					}).collect(Collectors.toList());

			FederateGroupHopLimits limits = rol.getFederateGroupHopLimits().toBuilder().clearLimits()
					.addAllLimits(updatedLimits).build();

			builder.setFederateGroupHopLimits(limits);

			// add message maxHops
			FederateHops federateHops = FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
			builder.setFederateHops(federateHops);

			federateProvenances.addAll(rol.getFederateProvenanceList());
			builder.clearFederateProvenance();
			builder.addAllFederateProvenance(federateProvenances);

			return (T) builder.build();
		}

		if (event instanceof BinaryBlob) {
			BinaryBlob blob = (BinaryBlob) event;
			BinaryBlob.Builder builder = blob.toBuilder();

			// update limits
			List<FederateGroupHopLimit> updatedLimits = blob.getFederateGroupHopLimits().getLimitsList().stream()
					.map(limit -> {
						return limit.toBuilder().setCurrentHops(limit.getCurrentHops() + 1).build();
					}).collect(Collectors.toList());

			FederateGroupHopLimits limits = blob.getFederateGroupHopLimits().toBuilder().clearLimits()
					.addAllLimits(updatedLimits).build();

			builder.setFederateGroupHopLimits(limits);

			// add message maxHops
			FederateHops federateHops = FederateHops.newBuilder().setCurrentHops(1).setMaxHops(maxFederateHops).build();
			builder.setFederateHops(federateHops);

			federateProvenances.addAll(blob.getFederateProvenanceList());
			builder.clearFederateProvenance();
			builder.addAllFederateProvenance(federateProvenances);

			return (T) builder.build();
		}

		return null;
	}

	public void throwDeadlineExceptionToClient() {
		try {
			if (clientStream != null) {
				clientStream.onError(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));
			}
			if (clientCall != null) {
				clientCall.cancel(Status.DEADLINE_EXCEEDED.getDescription(),
						new StatusRuntimeException(Status.DEADLINE_EXCEEDED));
			}
		} catch (Exception e) {
			logger.warn("exception sending StatusRuntimeException - DEADLINE_EXCEEDED to client", e);
		}
	}

	public void throwCanceledExceptionToClient() {
		try {
			if (clientStream != null) {
				clientStream.onError(new StatusRuntimeException(Status.CANCELLED));
			}

			if (clientCall != null) {
				clientCall.cancel(Status.CANCELLED.getDescription(), new StatusRuntimeException(Status.CANCELLED));
			}
		} catch (Exception e) {
			logger.warn("exception sending StatusRuntimeException - CANCELLED to client", e);
		}
	}

	public void throwPermissionDeniedToClient() {
		try {
			if (clientStream != null) {
				clientStream.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
			}
			if (clientCall != null) {
				clientCall.cancel(Status.PERMISSION_DENIED.getDescription(),
						new StatusRuntimeException(Status.PERMISSION_DENIED));
			}
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

	@Override
	public String toString() {
		return "GuardedStreamHolder [clientStream=" + clientStream + ", clientCall=" + clientCall + ", lastHealthTime="
				+ lastHealthTime + ", lastHealthStatus=" + lastHealthStatus + ", federateIdentity=" + federateIdentity
				+ ", subscription=" + subscription + "]";
	}
}
