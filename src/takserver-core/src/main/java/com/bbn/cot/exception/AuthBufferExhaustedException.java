

package com.bbn.cot.exception;

public class AuthBufferExhaustedException extends RuntimeException {

    private static final long serialVersionUID = -717207627263258767L;

    public AuthBufferExhaustedException() {
		super();
	}

	public AuthBufferExhaustedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AuthBufferExhaustedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthBufferExhaustedException(String message) {
		super(message);
	}

	public AuthBufferExhaustedException(Throwable cause) {
		super(cause);
	}
}
