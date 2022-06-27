

package com.bbn.marti.nio.util;

import java.io.IOException;

/**
* A specific exception for signalling that a given channel is not registered with a selector. 
*
* Used for singling out the case where a change is made to a selector, but the selectable channel is not registered.
*/
public class UnregisteredChannelException extends IOException {
	public UnregisteredChannelException(String msg) {
		super(msg);
	}
	
	public UnregisteredChannelException() {
		super();
	}
}