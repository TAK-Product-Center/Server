

package com.bbn.marti.remote.exception;

// General TAKServer exception
public class TakException extends RuntimeException {

    private static final long serialVersionUID = 6018970681826270384L;

    public TakException() {
		super();
	}

	public TakException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TakException(String message, Throwable cause) {
		super(message, cause);
	}

	public TakException(String message) {
		super(message);
	}

	public TakException(Throwable cause) {
		super(cause);
	}
}
