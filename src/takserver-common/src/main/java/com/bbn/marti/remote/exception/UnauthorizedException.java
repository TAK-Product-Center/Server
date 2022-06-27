

package com.bbn.marti.remote.exception;


public class UnauthorizedException extends TakException {

    private static final long serialVersionUID = -664085619547893136L;

    public UnauthorizedException() {
		super();
	}

	public UnauthorizedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnauthorizedException(String message) {
		super(message);
	}

	public UnauthorizedException(Throwable cause) {
		super(cause);
	}
}
