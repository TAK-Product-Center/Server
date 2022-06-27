package tak.server.federation.hub.broker;

import java.io.InputStream;

import mil.af.rl.rol.value.ResourceDetails;

/*
 * Value class for sync resources
 */
public class SyncResultStream {

    private ResourceDetails details;

    private InputStream inputStream;

    public ResourceDetails getDetails() {
        return details;
    }

    public void setDetails(ResourceDetails details) {
        this.details = details;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public SyncResultStream() { }

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
        SyncResultStream other = (SyncResultStream) obj;
        if (details == null) {
            if (other.details != null)
                return false;
        } else if (!details.equals(other.details))
            return false;
        return true;
    }
}
