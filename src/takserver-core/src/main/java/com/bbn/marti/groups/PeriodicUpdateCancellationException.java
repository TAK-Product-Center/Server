

package com.bbn.marti.groups;

/*
 * This exception will be explicity thrown to trigger cancellation of future periodic authz/n updates
 * 
 * 
 */
public class PeriodicUpdateCancellationException extends RuntimeException {
    
    private static final long serialVersionUID = 1166613161883596958L;

    public PeriodicUpdateCancellationException() {
        super();
    }

    public PeriodicUpdateCancellationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PeriodicUpdateCancellationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PeriodicUpdateCancellationException(String message) {
        super(message);
    }

    public PeriodicUpdateCancellationException(Throwable cause) {
        super(cause);
    }
    
}
