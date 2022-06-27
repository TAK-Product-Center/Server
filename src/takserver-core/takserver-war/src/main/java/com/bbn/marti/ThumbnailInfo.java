

package com.bbn.marti;

import java.io.Serializable;
import java.util.Date;

// POJO representing thumbnail metadata 
public class ThumbnailInfo implements Serializable {
	
	private static final long serialVersionUID = 876876283475682376L;
	private Integer dbId;
	private String uid;
	private Date timestamp;
	
	public ThumbnailInfo(Integer dbId, String uid, Date timestamp) {
		super();
		this.dbId = dbId;
		this.uid = uid;
		this.timestamp = timestamp;
	}
	public Integer getDbId() {
		return dbId;
	}
	public void setDbId(Integer dbId) {
		this.dbId = dbId;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dbId == null) ? 0 : dbId.hashCode());
		result = prime * result
				+ ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
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
		ThumbnailInfo other = (ThumbnailInfo) obj;
		if (dbId == null) {
			if (other.dbId != null)
				return false;
		} else if (!dbId.equals(other.dbId))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "ThumbnailInfo [dbId=" + dbId + ", uid=" + uid
				+ ", timestamp=" + timestamp + "]";
	}
}