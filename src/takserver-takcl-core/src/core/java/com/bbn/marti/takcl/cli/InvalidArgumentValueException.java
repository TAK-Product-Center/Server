package com.bbn.marti.takcl.cli;

/**
 * Exception for invalid argument values
 * <p>
 * Created on 8/25/17.
 */
public class InvalidArgumentValueException extends EndUserReadableException {
	public InvalidArgumentValueException(String command, String argumentIdentifier, String argumentValue) {
		super("Command \"" + command + "\" was provided an invalid value of \""
				+ argumentValue + "\" for the argument \"" + argumentIdentifier + "\"!");

	}
}
