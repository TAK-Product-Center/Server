package tak.server.federation;

/**
 * Created on 4/17/2017.
 */
public class FederationException extends Exception {
    private String additionalInformation;
    private static final long serialVersionUID = -7432430172543241383L;

    public FederationException(String additionalInformation) {
        super(additionalInformation);
        this.additionalInformation = additionalInformation;
    }

    public FederationException(Throwable failureCausingException) {
        super(failureCausingException);
    }

    public FederationException(String additionalInformation, Throwable failureCausingException) {
        super(additionalInformation, failureCausingException);
        this.additionalInformation = additionalInformation;
    }

    @Override
    public String toString() {
        Throwable cause = this.getCause();
        if(cause == null) {
            return this.getMessage();
        } else if (this.additionalInformation == null) {
            return cause.toString();
        }
        return additionalInformation + ", with a causing exception of: " + cause.toString();
    }
}
