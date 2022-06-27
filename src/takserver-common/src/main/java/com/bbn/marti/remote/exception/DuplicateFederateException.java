

package com.bbn.marti.remote.exception;


public class DuplicateFederateException extends TakException {

    private static final long serialVersionUID = 4447125412297136165L;

    public DuplicateFederateException() {
		super();
	}

	public DuplicateFederateException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DuplicateFederateException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateFederateException(String message) {
		super(message);
	}

	public DuplicateFederateException(Throwable cause) {
		super(cause);
	}
}
