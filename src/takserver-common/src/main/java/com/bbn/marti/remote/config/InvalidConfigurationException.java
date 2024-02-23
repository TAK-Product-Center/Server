package com.bbn.marti.remote.config;

public class InvalidConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1958469146663938921L;

	public InvalidConfigurationException(String message) {
        super(message);
    }
}