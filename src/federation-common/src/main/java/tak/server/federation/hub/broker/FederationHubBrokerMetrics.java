package tak.server.federation.hub.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;

public class FederationHubBrokerMetrics {
	
	@JsonIgnore
	private long totalWrites = 0l;
	
	@JsonIgnore
	private long totalReads = 0l;

    @JsonIgnore
    private long totalBytesWritten = 0l;

    @JsonIgnore
    private long totalBytesRead = 0l;
	
	@JsonIgnore
	private long currentLatency = 0l;
	
	@JsonIgnore
	private long currentWrites = 0l;

	public void incrementTotalReads() {
        totalReads++;
	}
	
	public void incrementTotalWrites(long messageSize, long latency) {
        totalBytesWritten += messageSize;
		totalWrites++;
		// these are reset on the cloudwatch interval so that we can track the average over that interval
		currentWrites++;
		currentLatency+= latency;
	}
	
	public void resetLatency() {
		currentWrites = 0l;
		currentLatency = 0l;
	}

    public void setTotalBytesRead(long totalBytesRead) {
        this.totalBytesRead = totalBytesRead;
    }
	
	@JsonProperty("totalReads")
	public long getTotalReads() {
		return totalReads;
	}
	
	@JsonProperty("totalWrites")
	public long getTotalWrites() {
		return totalWrites;
	}

    @JsonProperty("totalBytesRead")
    public long getTotalBytesRead() { return totalBytesRead; }

    @JsonProperty("totalBytesWritten")
    public long getTotalBytesWritten() { return totalBytesWritten; }
	
	@JsonProperty("averageLatencyMs")
	public double averageLatencyMs() {
		return currentWrites == 0 ? 0.0 : (double) currentLatency / (double) currentWrites;
	}
	
	@JsonProperty("averageLatencyNs")
	public long getAverageLatencyNs() {
		return currentWrites == 0 ? 0 : (currentLatency * 1_000_000) / (currentWrites);
	}
	
	private long totalMessagesDropped = 0;
    public long getTotalMessagesDropped() {
		return totalMessagesDropped;
	}

	public void setTotalMessagesDropped(long totalMessagesDropped) {
		this.totalMessagesDropped = totalMessagesDropped;
	}

	@JsonIgnore
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> channelInfosInternal = new ConcurrentHashMap<>();


    private final Set<ChannelInfo> channelInfos = new ConcurrentSkipListSet<ChannelInfo>();

    public ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> getChannelInfosInternal() {
        return new ConcurrentHashMap<>(channelInfosInternal);
    }

    public List<ChannelInfo> getChannelInfos() {
        return new ArrayList<ChannelInfo>(channelInfos);
    }

    public void incrementChannelWrite(String sourceId,
                                      String sourceCert,
                                      String targetId,
                                      String targetCert,
                                      long messageLength) {
        if (!channelInfosInternal.containsKey(sourceCert)) {
            channelInfosInternal.put(sourceCert, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<String, ChannelInfo> channelInfo = channelInfosInternal.get(sourceCert);
        if (!channelInfo.containsKey(targetCert)) {
            ChannelInfo newChannelInfo = new ChannelInfo(sourceId, sourceCert, targetId, targetCert);
            channelInfo.put(targetCert, newChannelInfo);
            channelInfos.add(newChannelInfo);
        }

        // update the id's to the latest
        channelInfo.get(targetCert).setTargetId(targetId);
        channelInfo.get(targetCert).setSourceId(sourceId);

        channelInfo.get(targetCert).messagesWritten += 1;
        channelInfo.get(targetCert).bytesWritten += messageLength;
        channelInfo.get(targetCert).totalMessages += 1;
    }

    public void incrementChannelRead(String targetId,
                                     String targetCert,
                                     String sourceId,
                                     String sourceCert,
                                     long messageLength) {
        if (!channelInfosInternal.containsKey(sourceCert)) {
            channelInfosInternal.put(sourceCert, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<String, ChannelInfo> channelInfo = channelInfosInternal.get(sourceCert);
        if (!channelInfo.containsKey(targetCert)) {
            ChannelInfo newChannelInfo = new ChannelInfo(sourceId, sourceCert, targetId, targetCert);
            channelInfo.put(targetCert, newChannelInfo);
            channelInfos.add(newChannelInfo);
        }

        // update the id's to the latest
        channelInfo.get(targetCert).setTargetId(targetId);
        channelInfo.get(targetCert).setSourceId(sourceId);

        channelInfo.get(targetCert).messagesRead += 1;
        channelInfo.get(targetCert).bytesRead += messageLength;
        channelInfo.get(targetCert).totalMessages += 1;
    }

    public static class ChannelInfo implements Comparable<ChannelInfo> {

        public ChannelInfo(String sourceId,
                           String sourceCert,
                           String targetId,
                           String targetCert) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.sourceCert = sourceCert;
            this.targetCert = targetCert;
        }

        public long getMessagesRead() {
            return messagesRead;
        }

        public void setMessagesRead(long messagesRead) {
            this.messagesRead = messagesRead;
        }

        public long getTotalMessages() {
            return totalMessages;
        }

        public void setTotalMessages(long totalMessages) {
            this.totalMessages = totalMessages;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        public void setBytesRead(long bytesRead) {
            this.bytesRead = bytesRead;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getSourceCert() {
            return sourceCert;
        }

        public void setSourceCert(String sourceCert) {
            this.sourceCert = sourceCert;
        }

        public String getTarget() {
            return targetCert;
        }

        public void setTarget(String target) {
            this.targetCert = target;
        }

        public long getMessagesWritten() {
            return messagesWritten;
        }

        public void setMessagesWritten(long messagesWritten) {
            this.messagesWritten = messagesWritten;
        }

        public String sourceId;
        public String targetId;
        public String sourceCert;
        public String targetCert;
        public long messagesWritten;
        public long messagesRead;
        public long totalMessages;
        public long bytesWritten;
        public long bytesRead;

        @Override
        public String toString() {
            return "ChannelInfo{" +
                    "sourceId='" + sourceId + '\'' +
                    ", targetId='" + targetId + '\'' +
                    ", sourceCert='" + sourceCert + '\'' +
                    ", targetCert='" + targetCert + '\'' +
                    ", messagesWritten=" + messagesWritten +
                    ", messagesRead=" + messagesRead +
                    ", totalMessages=" + totalMessages +
                    ", bytesWritten=" + bytesWritten +
                    ", bytesRead=" + bytesRead +
                    '}';
        }
		@Override
		public int compareTo(ChannelInfo that) {
			return ComparisonChain.start().compare(this.sourceId, that.sourceId)
						.compare(this.targetId, that.targetId)
						.compare(this.getSourceCert(), that.getSourceCert())
						.compare(this.getTarget(), that.getTarget())
						.result();
		}
    }

    @Override
    public String toString() {
        return "FederationHubBrokerMetrics{" +
                "channelWriteInfosInternal=" + channelInfosInternal +
                ", channelInfos=" + channelInfos +
                '}';
    }
}