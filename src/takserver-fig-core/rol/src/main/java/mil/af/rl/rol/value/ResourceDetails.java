package mil.af.rl.rol.value;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 
 * Value class representing enterprise sync resource details
 * 
 */
public class ResourceDetails extends Parameters {
  
    private static final long serialVersionUID = -764158418661444241L;

    private String sha256;
    
    private String senderUid;
   
    private String name;
    
    private String filename;
    
    private String senderUrl;
    
    private Date tsStored; // timestamp when the resource was saved
    
    @JsonProperty("sizeInBytes")
    private int size;
        
    private String senderCallsign;
    
    @JsonIgnore
    private String localPath;
    
    public ResourceDetails() { }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSenderUrl() {
        return senderUrl;
    }

    public void setSenderUrl(String senderUrl) {
        this.senderUrl = senderUrl;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSenderCallsign() {
        return senderCallsign;
    }

    public void setSenderCallsign(String senderCallsign) {
        this.senderCallsign = senderCallsign;
    }
    
    public Date getTsStored() {
        return tsStored;
    }

    public void setTsStored(Date tsStored) {
        this.tsStored = tsStored;
    }
    
    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sha256 == null) ? 0 : sha256.hashCode());
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
        ResourceDetails other = (ResourceDetails) obj;
        if (sha256 == null) {
            if (other.sha256 != null)
                return false;
        } else if (!sha256.equals(other.sha256))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceDetails [sha256=");
        builder.append(sha256);
        builder.append(", senderUid=");
        builder.append(senderUid);
        builder.append(", name=");
        builder.append(name);
        builder.append(", filename=");
        builder.append(filename);
        builder.append(", senderUrl=");
        builder.append(senderUrl);
        builder.append(", tsStored=");
        builder.append(tsStored);
        builder.append(", size=");
        builder.append(size);
        builder.append(", senderCallsign=");
        builder.append(senderCallsign);
        builder.append(", localPath=");
        builder.append(localPath);
        builder.append("]");
        return builder.toString();
    }
}
