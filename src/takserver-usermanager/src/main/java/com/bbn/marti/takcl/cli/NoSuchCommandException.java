package com.bbn.marti.takcl.cli;

/**
 * Created on 8/18/17.
 */
public class NoSuchCommandException extends EndUserReadableException {

	public NoSuchCommandException(String command) {
		super("\"" + command + "\" is not a valid command!");
	}
}
