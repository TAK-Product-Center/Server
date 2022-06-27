

package com.bbn.cot.exception;

public class AuthenticationFailedException extends RuntimeException {

    private static final long serialVersionUID = -717207627263258767L;

    public AuthenticationFailedException() {
		super();
	}

	public AuthenticationFailedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AuthenticationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationFailedException(String message) {
		super(message);
	}

	public AuthenticationFailedException(Throwable cause) {
		super(cause);
	}
}
