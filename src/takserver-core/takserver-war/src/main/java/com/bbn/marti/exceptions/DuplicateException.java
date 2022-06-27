

package com.bbn.marti.exceptions;

public class DuplicateException extends RuntimeException {

	private static final long serialVersionUID = 1341234890L;

    public DuplicateException() {
		super();
	}

	public DuplicateException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DuplicateException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateException(String message) {
		super(message);
	}

	public DuplicateException(Throwable cause) {
		super(cause);
	}
}
