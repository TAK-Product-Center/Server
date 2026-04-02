package com.bbn.marti.takcl.AppModules.generic;

import com.bbn.marti.takcl.cli.EndUserReadableException;

/**
 * Created on 9/28/15.
 */
public interface AppModuleInterface extends BaseAppModuleInterface {

	/**
	 * Initializes the module. If it has already been initialized this method should do nothing.
	 */
	void init() throws EndUserReadableException;

}
