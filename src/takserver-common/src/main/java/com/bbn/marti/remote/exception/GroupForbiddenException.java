

package com.bbn.marti.remote.exception;

public class GroupForbiddenException extends RuntimeException {

	private static final long serialVersionUID = 76628332657L;

	public GroupForbiddenException() {
		super();
	}

	public GroupForbiddenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public GroupForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	public GroupForbiddenException(String message) {
		super(message);
	}

	public GroupForbiddenException(Throwable cause) {
		super(cause);
	}
}
