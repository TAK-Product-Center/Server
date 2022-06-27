

package com.bbn.marti.remote.exception;

/*
 * 
 * Exception indicating an error condition that may be resolved by trying again.
 * 
 */
public class RetryableException extends RuntimeException {

    private static final long serialVersionUID = -623892349234598L;

    public RetryableException() {
		super();
	}

	public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RetryableException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryableException(String message) {
		super(message);
	}

	public RetryableException(Throwable cause) {
		super(cause);
	}
}
