package com.bbn.marti.takcl.cli;

/**
 * Created on 8/18/17.
 */
public class NoSuchCommandArgumentException extends EndUserReadableException {
	public NoSuchCommandArgumentException(String command, String argument) {
		super("\"" + argument + "\" is not a valid argument for the command \"" + command + "\"!");

	}
}
