

package com.bbn.marti.util.concurrent.executor;

import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

/**
* A concrete Ordered Executor implementation that places no restrictions
* on the size of the core's queue
*/
public class UnsizedOrderedExecutor extends SizableOrderedExecutor {
	private final static Logger log = Logger.getLogger(UnsizedOrderedExecutor.class);
	
	public UnsizedOrderedExecutor(AsyncExecutor delegate, String name) {
		super(delegate, name);
	}
	
	protected boolean incrementCount() {
		return true;
	}
	
	protected void decrementCount() {
		;
	}
}