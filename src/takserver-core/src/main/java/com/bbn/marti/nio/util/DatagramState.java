

package com.bbn.marti.nio.util;

import java.util.EnumSet;
import java.util.Set;

/**
* Set of states for a connectionless protocol, with no session layer protocols (that the handler is aware of)
*
* INIT is used to guard against errors during construction/initialization of the handler/listener/stream processing chain
* 
* OPEN/CLOSED represent the network state of a datagram socket. CLOSING is used to differentiate between the time when the application
* calls close, but data has yet to be written out; the application is no longer aware of the handler during closing.
*
* At CLOSING -> CLOSED, network resources are released.
*
* If a forceClose is issued from OPEN -> CLOSED, all pending write data is dropped, in addition to any actions taken in CLOSING -> CLOSED
*/
public enum DatagramState {
    INIT, // handler is built, registration/instantiation in progress
    OPEN, // fully connected--can receive application writes, deliver read traffic to the application
    CLOSING, // in process of closing, initiated from the application side 
    CLOSED; // fully closed -- no more network or application traffic.

    // states in which a (client) handler can receive a connect call (ChannelHandler.connect) from the application
    public final static Set<DatagramState> connectReceiveStates = EnumSet.of(INIT, CLOSED);

    // states in which a handler can receive a write call-in (handleWrite) from the server
    public final static Set<DatagramState> writeHandleStates = EnumSet.of(OPEN, CLOSING);

    // states in which a handler can receive write data (ChannelHandler.write) from the application
    public final static Set<DatagramState> writeApplicationReceiveStates = EnumSet.of(OPEN);

    // states in which a handler can receive a close call (ChannelHandler.close) from the application
    public final static Set<DatagramState> closeReceiveStates = EnumSet.of(OPEN, CLOSING);
    
    // states in which a handler should expect that the network connection is active
    public final static Set<DatagramState> netConnectStates = EnumSet.of(OPEN, CLOSING);

    // states in which a handler should not apply a force close ie. it's already done
    public final static Set<DatagramState> forceClosedStates = EnumSet.of(CLOSED);
}