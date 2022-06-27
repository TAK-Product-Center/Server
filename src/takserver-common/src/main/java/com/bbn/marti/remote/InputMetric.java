

package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.bbn.marti.config.Network;
import com.google.common.collect.ComparisonChain;

/**
 * Created on 9/25/15.
 */
public class InputMetric implements Serializable, Comparable<InputMetric> {
 
    private static final long serialVersionUID = 6965608049845317917L;

    private final String id = UUID.randomUUID().toString().replace("-", "");
    
    private Network.Input input;
    private AtomicLong readsReceived = new AtomicLong();
    private AtomicLong messagesReceived = new AtomicLong();
    private AtomicLong numClients = new AtomicLong();
    
    public InputMetric(Network.Input input) {
        if (input == null) {
            throw new IllegalArgumentException("null input");
        }
        
        this.input = input;
    }
    
    public Network.Input getInput() {
        return input;
    }
    
    public void setInput(Network.Input input) {
        this.input = input;
    }
    
    public AtomicLong getMessagesReceived() {
        return messagesReceived;
    }
    
    public void setMessagesReceived(AtomicLong messagesReceived) {
        this.messagesReceived = messagesReceived;
    }

    public AtomicLong getReadsReceived() {
        return readsReceived;
    }

    public void setReadsReceived(AtomicLong readsReceived) {
        this.readsReceived = readsReceived;
    }

    public AtomicLong getNumClients() {
        return numClients;
    }

    public void setNumClients(AtomicLong numClients) {
        this.numClients = numClients;
    }

    public String getId() {
        return id;
    }
    
    @Override
    public int compareTo(InputMetric that) {
        return ComparisonChain.start().compare(this.getId(), that.getId()).result();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        InputMetric other = (InputMetric) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "InputMetric [id=" + id + ", input=" + input
                + ", readsReceived=" + readsReceived + ", messagesReceived="
                + messagesReceived + ", numClients=" + numClients + "]";
    }
}
