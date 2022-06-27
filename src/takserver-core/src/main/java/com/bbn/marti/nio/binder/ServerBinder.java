

package com.bbn.marti.nio.binder;

import java.io.IOException;

import com.bbn.marti.nio.server.ChannelWrapper;
import com.bbn.marti.nio.server.Server;

/**
* Interface for an object that creates a channel/channel handler/interest ops triple when a server
* calls into its bind method.
*
* Intended for delayed/repeatable binding, so that the server can easily be turned
* on/off when that capability is added.
*
* Implementors should be static/pure with respect to multiple calls to handleBind, and should
* ensure any necessary tear-downs internally in the case of exceptions.
*/
public interface ServerBinder {
	public ChannelWrapper handleBind(Server server) throws IOException;
}
