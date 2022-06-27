package com.bbn.marti.exceptions;

import org.springframework.security.core.AuthenticationException;

public class CoreCommunicationException extends AuthenticationException {

    private static final long serialVersionUID = 1987987194L;

    public CoreCommunicationException(String msg, Throwable t) {
        super(msg, t);
    }

    public CoreCommunicationException(String msg) {
        super(msg);
    }
}
