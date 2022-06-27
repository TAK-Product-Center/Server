

package com.bbn.marti.service.kml;

public class KMLServiceException extends RuntimeException {

	private static final long serialVersionUID = -876823462341L;

	public KMLServiceException() {
		super();
	}

	public KMLServiceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public KMLServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public KMLServiceException(String message) {
		super(message);
	}

	public KMLServiceException(Throwable cause) {
		super(cause);
	}

}
