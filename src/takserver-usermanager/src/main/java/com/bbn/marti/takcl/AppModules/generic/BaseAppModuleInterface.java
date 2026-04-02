package com.bbn.marti.takcl.AppModules.generic;

import com.bbn.marti.takcl.config.common.TakclRunMode;
import org.jetbrains.annotations.NotNull;

/**
 * This defines the basic methods necessary for an AppModule
 * <p>
 * Created on 11/3/17.
 */
public interface BaseAppModuleInterface {

	/**
	 * An indication of the required server state, if applicable. RUNNING is typically used when it connects to a remote
	 * server, while STOPPED is typically used when a CoreConfig.xml file is being modified prior to server startup.
	 */
	enum ServerState {
		RUNNING,
		STOPPED,
		NOT_APPLICABLE
	}

	/**
	 * This defines how the module interacts with the server. It drives what checks are made against the configuration
	 * file among other validation.
	 *
	 * @return The run mode
	 */
	@NotNull
	TakclRunMode[] getRunModes();

	/**
	 * This defines the state the server must be in in order for this module to function
	 *
	 * @return The required server state
	 */
	AppModuleInterface.ServerState getRequiredServerState();

	/**
	 * This is the description that is displayed for the end user when this module class is used as a CLI component
	 *
	 * @return The command description
	 */
	String getCommandDescription();
}
