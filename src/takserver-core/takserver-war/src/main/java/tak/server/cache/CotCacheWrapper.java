package tak.server.cache;

import java.io.Serializable;

import com.bbn.marti.sync.model.UidDetails;
import com.google.common.collect.ComparisonChain;

import tak.server.cot.CotElement;

public class CotCacheWrapper implements Serializable, Comparable<CotCacheWrapper>{
	
	private static final long serialVersionUID = 4702356451469519388L;
	private CotElement cotElement;
	private UidDetails uidDetails;
	private String uid;
	private String groupsBitVectorString;
	
	public CotElement getCotElement() {
		return cotElement;
	}
	
	public void setCotElement(CotElement cotElement) {
		this.cotElement = cotElement;
	}
	
	public UidDetails getUidDetails() {
		return uidDetails;
	}
	
	public void setUidDetails(UidDetails uidDetails) {
		this.uidDetails = uidDetails;
	}
	
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
	public String getGroupsBitVectorString() {
		return groupsBitVectorString;
	}

	public void setGroupsBitVectorString(String groupsBitVectorString) {
		this.groupsBitVectorString = groupsBitVectorString;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cotElement == null) ? 0 : cotElement.hashCode());
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
		result = prime * result + ((uidDetails == null) ? 0 : uidDetails.hashCode());
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
		CotCacheWrapper other = (CotCacheWrapper) obj;
		if (cotElement == null) {
			if (other.cotElement != null)
				return false;
		} else if (!cotElement.equals(other.cotElement))
			return false;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		if (uidDetails == null) {
			if (other.uidDetails != null)
				return false;
		} else if (!uidDetails.equals(other.uidDetails))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CotCacheWrapper [cotElement=" + cotElement + ", uidDetails=" + uidDetails + ", uid=" + uid + "]";
	}

	@Override
	public int compareTo(CotCacheWrapper o) {
		return ComparisonChain.start().compare(getCotElement().hashCode(), o.getCotElement().hashCode()).result();
	}
}
