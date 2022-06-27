package com.bbn.marti.remote.exception;


public class MissionDeletedException extends RuntimeException {

    private static final long serialVersionUID = 7652374658L;

    public MissionDeletedException() {
        super();
    }

    public MissionDeletedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MissionDeletedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissionDeletedException(String message) {
        super(message);
    }

    public MissionDeletedException(Throwable cause) {
        super(cause);
    }
}
