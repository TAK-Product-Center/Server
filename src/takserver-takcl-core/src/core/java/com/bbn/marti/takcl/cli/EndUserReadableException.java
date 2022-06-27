package com.bbn.marti.takcl.cli;

import org.jetbrains.annotations.NotNull;

/**
 * Exception for invalid argument values
 * <p>
 * Created on 8/25/17.
 */
public class EndUserReadableException extends RuntimeException {

	public final String displayMessage;

	public EndUserReadableException(@NotNull String displayMessage, @NotNull Exception e) {
		super(displayMessage, e);
		this.displayMessage = displayMessage;
	}

	public EndUserReadableException(@NotNull String displayMessage) {
		super(displayMessage);
		this.displayMessage = displayMessage;
	}
}
