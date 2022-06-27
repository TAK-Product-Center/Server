

package com.bbn.tak.schema;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Print help")
public class HelpCommand extends Command {

	public static final String name = "help";
	
	HelpCommand(SchemaManager manager) {
		super(manager, LoggerFactory.getLogger(HelpCommand.class));
	}

	/**
	 * Prints help.
	 */
	@Override
	public boolean execute() {
		schemaManager.jcommander.usage();
		return true;
	}

}
