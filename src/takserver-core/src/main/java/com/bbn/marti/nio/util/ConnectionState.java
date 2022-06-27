

package com.bbn.marti.nio.util;

import java.util.EnumSet;
import java.util.Set;

/**
* An enumeration for representing the states of a connection-oriented IO resource, with 
* session layer protocols implemented above the transport layer (see APP_CONNECT and CLOSING)
*
* The connection-oriented network states are NET_CONNECT, OPEN, and CLOSED, roughly representing the connecting,
* open, and closed phases of tcp-like protocols
*
* In addition, we use APP_CONNECT, CLOSING, and EOS to represent phases where the session layer protocols
* are connecting (APP_CONNECT), closing with respect to the network client (EOS), and closing with respect
* to the application client (EOS -- specifically the phase where the network has closed, but we have yet to
* deliver the stream end to the application)
*
* INIT is used to guard against accidental calls made in the construction phase of the channel/handler/pipeline/stream processor.
* 
* The session layer protocols are denotated as the "pipeline".
*
* The _handle, _pipelineReceive, and _applicationRecieve state sets each contain the event + layer 
* pair in which it is acceptable for that particular event to continue to propagate. The handle layer
* is the set of calls we allow coming in from the server, the pipeline layer is the set of calls allowed
* coming out of the session layer, and the receive layer is the set of calls coming from the application.
*/
public enum ConnectionState {
    INIT, // handler is built, but registration/instantiation in progress, ie. the handler is not aware of the connection
    NET_CONNECT, // in process of connecting via the network--have not called finishConnect yet, ie. the pipeline is not aware of the handler
    APP_CONNECT, // in process of propagating connect through the pipeline--have not yet called onConnect on the ChannelListener, ie. the listener is not aware of the handler
    OPEN, // fully connected--can receive application writes, deliver read traffic to the application. Both the pipeline/listener are aware of the handler
    APP_CLOSED, // in process of closing, initiated from the application side. The Listener is not aware of the handler in the application -> network direction
    NET_CLOSED, // network side completely closed -- potentially still have pending network -> application data in progress. To be followed by onInboundCloseComplete -> onInboundClose. The pipeline is not aware of the handler in the application -> network direction
    CLOSED; // fully closed -- no more network or application traffic. The listener and pipeline are not aware of the handler in either direction

    // states in which a handler can receive a read call-in (handleRead) from the server
    public final static Set<ConnectionState> readHandleStates = EnumSet.of(APP_CONNECT, OPEN, APP_CLOSED);

    // states in which a handler can pass data to an application-side ChannelListener
    public final static Set<ConnectionState> readBroadcastStates = EnumSet.of(OPEN, APP_CLOSED);

    // states in which a handler can receive a write call-in (handleWrite) from the server
    public final static Set<ConnectionState> writeHandleStates = readHandleStates;

    // states in which a handler can receive write data (onEncodeComplete) from the pipeline
    public final static Set<ConnectionState> writePipelineReceiveStates = readHandleStates;

    // states in which a handler can receive write data (ChannelHandler.write) from the application
    public final static Set<ConnectionState> writeApplicationReceiveStates = EnumSet.of(OPEN);

    // states in which a handler can receive a connect call-in (handleConnect) from the server
    public final static Set<ConnectionState> connectHandleStates = EnumSet.of(NET_CONNECT);

    // states in which a (client) handler can process a connect call (ChannelHandler.connect) from the application
    public final static Set<ConnectionState> connectReceiveStates = EnumSet.of(INIT, CLOSED);

    // states in which a handler will receive no more network traffic (and in which a forceClose/Close will be idempotent)
    public final static Set<ConnectionState> eosedStates = EnumSet.of(NET_CLOSED, CLOSED);
    
    // states in which a close is in progress or has already occurred (ie, a close call will not apply -- a force close call is different)
    public final static Set<ConnectionState> closingStates = EnumSet.of(APP_CLOSED, NET_CLOSED, CLOSED);

    // states in which a delicate close operation may be in progress -- if disrupted, no progress can be made
    public final static Set<ConnectionState> panicStates = EnumSet.of(APP_CLOSED, NET_CLOSED);

    // states in which a handler should expect that the network connection is active
    public final static Set<ConnectionState> netConnectStates = EnumSet.of(NET_CONNECT, APP_CONNECT, OPEN, APP_CLOSED);
}