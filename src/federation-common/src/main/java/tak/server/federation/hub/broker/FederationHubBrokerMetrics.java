package tak.server.federation.hub.broker;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FederationHubBrokerMetrics {

    @JsonIgnore
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> channelInfosInternal = new ConcurrentHashMap<>();


    private final List<ChannelInfo> channelInfos = new ArrayList<>();

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

    public static class ChannelInfo {

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
    }

    @Override
    public String toString() {
        return "FederationHubBrokerMetrics{" +
                "channelWriteInfosInternal=" + channelInfosInternal +
                ", channelInfos=" + channelInfos +
                '}';
    }
}