
package tak.server.federation.hub.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;

public class FederationHubBrokerGlobalMetrics {

    @JsonIgnore
    private double heapAllocated = 0l;

    @JsonIgnore
    private double heapUtilized = 0l;

    @JsonIgnore
    private double cpuUtilized = 0l;

    @JsonIgnore
    private int cpuCores = 0;

    @JsonIgnore
    private int numConnectedClients = 0;

    @JsonIgnore
    private double writesPerSecond = 0l;

    @JsonIgnore
    private double readsPerSecond = 0l;

    @JsonIgnore
    private double bytesWrittenPerSecond = 0l;

    @JsonIgnore
    private double bytesReadPerSecond = 0l;

    @JsonProperty("heapAllocated")
    public double getHeapAllocated() {
        return heapAllocated;
    }

    @JsonProperty("heapUtilized")
    public double getHeapUtilized() {
        return heapUtilized;
    }

    @JsonProperty("cpuUtilized")
    public double getCpuUtilized() { return cpuUtilized; }

    @JsonProperty("cpuCores")
    public int getCpuCores() { return cpuCores; }

    @JsonProperty("numConnectedClients")
    public int getNumConnectedClients() { return numConnectedClients; }

    @JsonProperty("writesPerSecond")
    public double getWritesPerSecond() { return writesPerSecond; }

    @JsonProperty("readsPerSecond")
    public double getReadsPerSecond() { return readsPerSecond; }

    @JsonProperty("bytesWrittenPerSecond")
    public double getBytesWrittenPerSecond() { return bytesWrittenPerSecond; }

    @JsonProperty("bytesReadPerSecond")
    public double getBytesReadPerSecond() { return bytesReadPerSecond; }

    public void setHeapAllocated(double l) {
        heapAllocated = l;
    }

    public void setHeapUtilized(double l) {
        heapUtilized = l;
    }

    public void setCpuUtilized(double l) { cpuUtilized = l; }

    public void setCpuCores(int i) { cpuCores = i; }

    public void setNumConnectedClients(int i) { numConnectedClients = i; }

    public void setWritesPerSecond(double d) {
        writesPerSecond = d;
    }

    public void setReadsPerSecond(double d) {
        readsPerSecond = d;
    }

    public void setBytesWrittenPerSecond(double d) {
        bytesWrittenPerSecond = d;
    }

    public void setBytesReadPerSecond(double d) {
        bytesReadPerSecond = d;
    }

    @Override
    public String toString() {
        return "FederationHubBrokerGlobalMetrics{" +
                "heapAllocated=" + heapAllocated +
                ", heapUtilized=" + heapUtilized +
                ", cpuUtilized=" + cpuUtilized +
                ", cpuCores=" + cpuCores +
                ", numConnectedClients=" + numConnectedClients +
                ", writesPerSecond=" + writesPerSecond +
                ", readsPerSecond=" + readsPerSecond +
                ", bytesWrittenPerSecond= " + bytesWrittenPerSecond +
                ", bytesReadPerSecond= " + bytesReadPerSecond +
                '}';
    }
}