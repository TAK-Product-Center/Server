package tak.server.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterMessageWrapper {
	
	private final Map<String,Object> context = new ConcurrentHashMap<>();
	
	private String messagePayload;
	
	private String sourceNode;

	public String getMessagePayload() {
		return messagePayload;
	}

	public void setMessagePayload(String messagePayload) {
		this.messagePayload = messagePayload;
	}

	public Map<String, Object> getContext() {
		return context;
	}
	
	public String getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(String sourceNode) {
		this.sourceNode = sourceNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((messagePayload == null) ? 0 : messagePayload.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterMessageWrapper other = (ClusterMessageWrapper) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (messagePayload == null) {
			if (other.messagePayload != null)
				return false;
		} else if (!messagePayload.equals(other.messagePayload))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ClusterMessageWrapper [context=" + context + ", messagePayload=" + messagePayload + "]";
	}
}
