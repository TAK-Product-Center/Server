package tak.server.federation.hub.broker.events;

import java.util.List;

public class StreamReadyEvent<T> extends BrokerServerEvent {
	private static final long serialVersionUID = 5023824657075431825L;

	public enum StreamReadyType {
		EVENT, GROUPS, ROL
	}
	
	private final StreamReadyType type;
	private final String streamKey;
	private final List<T> events;
	
	public StreamReadyEvent(Object source, StreamReadyType type, String streamKey, List<T> events) {
		super(source);
		this.type = type;
		this.streamKey = streamKey;
		this.events = events;
	}

	public StreamReadyType getType() {
		return type;
	}

	public String getStreamKey() {
		return streamKey;
	}

	public List<T> getEvents() {
		return events;
	}
}
