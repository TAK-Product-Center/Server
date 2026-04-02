package com.bbn.marti.takcl.AppModules.generic;

import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;

/**
 * This is similar to the implemented {@link AppModuleInterface}, but requires a server profile as part of initialization.
 * <p>
 * Created on 12/3/15.
 */
public interface ServerAppModuleInterface extends BaseAppModuleInterface {

	/**
	 * Initializes the module. If it has already been initialized this method should do nothing.
	 *
	 * @param serverProfile The server profile to use
	 */
	void init(@NotNull AbstractServerProfile serverProfile) throws EndUserReadableException;

	/**
	 * Halts the module. Necessary to disconnect from an ignite cluster. If it has already been shut down this method
	 * should do nothing.
	 */
	void halt();
}
