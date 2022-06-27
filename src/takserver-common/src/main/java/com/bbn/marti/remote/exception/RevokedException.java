
package com.bbn.marti.remote.exception;


public class RevokedException extends TakException {

    private static final long serialVersionUID = -664085619547893136L;

    public RevokedException() {
        super();
    }

    public RevokedException(String message, Throwable cause,
                                 boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RevokedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RevokedException(String message) {
        super(message);
    }

    public RevokedException(Throwable cause) {
        super(cause);
    }
}
