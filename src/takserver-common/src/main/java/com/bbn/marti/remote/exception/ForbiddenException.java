package com.bbn.marti.remote.exception;

public class ForbiddenException extends TakException {

	private static final long serialVersionUID = 4635248532828804800L;

	public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message, Throwable cause,
                                 boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(Throwable cause) {
        super(cause);
    }
}
