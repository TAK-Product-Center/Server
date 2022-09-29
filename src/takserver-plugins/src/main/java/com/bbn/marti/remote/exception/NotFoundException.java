/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package com.bbn.marti.remote.exception;

public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7652374657L;

	public NotFoundException() {
		super();
	}

	public NotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException(Throwable cause) {
		super(cause);
	}
}
