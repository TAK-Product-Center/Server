

package com.bbn.tak.schema;

import org.slf4j.Logger;

/**
 * Base class for SchemaManager commands.
 * This provides a uniform interface and some common error-detection and handling logic.
 * Child classes will implement their business logic in the executeCommand() method.
 * 
 *
 */
public abstract class Command {
	
	final SchemaManager schemaManager;
	final Logger logger;
	
	Command(SchemaManager manager, Logger logger) {
		schemaManager = manager;
		this.logger = logger;
	}
	
	/**
	 * Runs the business logic implemented in the concrete subclass.
	 * @return <code>true</code> if the command completed successfully.
	 * @throws IllegalStateException if a database connection was needed, but not available.
	 */
	public abstract boolean execute() throws IllegalStateException;
}
