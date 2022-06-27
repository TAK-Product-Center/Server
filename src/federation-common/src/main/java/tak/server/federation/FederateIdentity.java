package tak.server.federation;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import java.util.HashSet;
import java.util.Set;

/*
 * Value class uniquely identifying a federate.
 *
 *
 */
@SuppressWarnings("PMD.IfStmtsMustUseBraces")
public class FederateIdentity implements Comparable<FederateIdentity> {
    private static final String FED_ID_ATTR = "fed_id";
    private final String fedId;

    public FederateIdentity(String fedId) {

        if (Strings.isNullOrEmpty(fedId)) {
            throw new IllegalArgumentException("empty federate identity fedId");
        }

        this.fedId = fedId;
    }

    @Override
    public int hashCode() {
        final int prime = 61;
        int result = 1;
        result = prime * result + ((fedId == null) ? 0 : fedId.hashCode());
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
        FederateIdentity other = (FederateIdentity) obj;
        if (fedId == null) {
            if (other.fedId != null)
                return false;
        } else if (!fedId.equals(other.fedId))
            return false;
        return true;
    }

    public String getFedId() {
        return fedId;
    }

    @Override
    public String toString() {
        return "FederateIdentity{" +
                "fedId='" + fedId + '\'' +
                '}';
    }

    @Override
    public int compareTo(FederateIdentity that) {
        return ComparisonChain.start().compare(fedId, that.getFedId()).result();
    }
}
