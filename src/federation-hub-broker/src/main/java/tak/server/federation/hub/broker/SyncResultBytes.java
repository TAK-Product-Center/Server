package tak.server.federation.hub.broker;

import mil.af.rl.rol.value.ResourceDetails;

/*
 * Value class for sync resources
 */
public class SyncResultBytes {

    private ResourceDetails details;

    private byte[] bytes;

    public ResourceDetails getDetails() {
        return details;
    }

    public void setDetails(ResourceDetails details) {
        this.details = details;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public SyncResultBytes() { }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SyncResultStream [details=");
        builder.append(details);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((details == null) ? 0 : details.hashCode());
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
        SyncResultBytes other = (SyncResultBytes) obj;
        if (details == null) {
            if (other.details != null)
                return false;
        } else if (!details.equals(other.details))
            return false;
        return true;
    }
}
