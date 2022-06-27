

package com.bbn.marti.cot.search.model;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.google.common.base.Strings;


/*
 * 
 * 
 * POJO which models CoT query requests 
 * 
 */
public class CotSearch {
    private CotSearchStatus status;

    private String id;
    
    private final Map<CotSearchStatus, Date> statusDateMap;

    private String message;

    private long bytesSent;
    
    private long count;
    
    private Date timestamp;
    
    private boolean active;
    
    // can track changes by assigning a random value to this field 
    private long tag;
    
    private Random random;
   
    public CotSearch(String id, String message) {
        if (Strings.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("empty id in constructor");
        }
        
        random = new SecureRandom();
        
        //  ordered map
        statusDateMap = new ConcurrentSkipListMap<>();
        
        setStatus(CotSearchStatus.NEW);
        statusDateMap.put(CotSearchStatus.NEW, new Date());
        setMessage(message);
        setId(id);
        setActive(true);
        
        setTag(random.nextLong());
    }

    public CotSearchStatus getStatus() {
        return status;
    }

    public void setStatus(CotSearchStatus status) {
        this.status = status;
        statusDateMap.put(status, new Date());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }
    
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @DateTimeFormat(iso=ISO.DATE)
    public Map<CotSearchStatus, Date> getStatusTimestamps() {
        return statusDateMap;
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    // only id field is considered for hashCode() and equals()
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
        CotSearch other = (CotSearch) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CotSearch [status=" + status + ", id=" + id
                + ", statusDateMap=" + statusDateMap + ", message=" + message
                + ", bytesSent=" + bytesSent + "]";
    }
    
    public void updateStatus(CotSearchStatus status, String message, Date timestamp, boolean isActive) {
        setStatus(status);
        setMessage(message);
        getStatusTimestamps().put(status, timestamp);
        setTimestamp(timestamp);
        setTag(random.nextLong());
        setActive(isActive);
    }
}
