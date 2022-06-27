

package com.bbn.marti.nio.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListenerInstantiator;
import com.bbn.marti.util.Assertion;
import com.google.common.base.Joiner;

/**
* A generic, decorated stream instantiator implementation for handling type-specialized
* instantiation of a channel handler, while allowing the ChannelHandler to remain type-agnostic
*
* Specifically, given a channel handler, this partially-instantiated factory will (*synchronously*):
*
* - construct a ChannelListener/Protocol<T> instance using the given ProtocolInstantiator<T>, and link it
* to the ChannelHandler (see ChannelHandler.listener).
*
* - construct a list of ProtocolListener<T> instances using the given ChannelHandler, Protocol<T> pair, and
* store them into the Protocol<T>. 
* @note the ordering of ProtocolListenerInstantiator<T> defines the (possibly flatMapped) ordering of ProtocolListener<T>
*
* - return
*
* @note if a Protocol<T> is null or throws an exception, the callee is responsible for handling a runtime exception,
* while if a ProtocolListener<T> is null or throws an exception, it is not included in the stream processing chain (TODO: justify this asymmetry)
*/
public class DecoratedStreamInstantiator<T> implements StreamInstantiator {
    private final static Logger log = Logger.getLogger(DecoratedStreamInstantiator.class);

	private ProtocolInstantiator<T> protocolInstantiator; // factory used to convert ChannelHandler -> Protocol<T>
	private LinkedBlockingQueue<ProtocolListenerInstantiator<T>> listenerInstantiators; // factories used for instantiating ProtocolListener<T>s from the ChannelHandler/Protocol<T> pair
    private String listenerInstantiatorsStr = ""; // cached string representation of the protocol listeners, for fast toString conversion
    
	public DecoratedStreamInstantiator<T> withProtocolInstantiator(ProtocolInstantiator<T> protocolInstantiator) {
        Assertion.notNull(protocolInstantiator);
        
		this.protocolInstantiator = protocolInstantiator;
		return this;
	}
	
	public DecoratedStreamInstantiator<T> withProtocolListenerInstantiators(LinkedBlockingQueue<ProtocolListenerInstantiator<T>> listenerInstantiators) {
        Assertion.notNull(listenerInstantiators);
        Assertion.areNotNull(listenerInstantiators);
        
		this.listenerInstantiators = listenerInstantiators;
        
        updateListenerStr();
        
		return this;
	}
    
    /**
    * Stores the current, joined list of ListenerInstantiators.toString
    */
    private void updateListenerStr() {
        this.listenerInstantiatorsStr = Joiner.on(", ").join(this.listenerInstantiators);
    }
	
	/**
	* Build all the parts with the instantiators, link together
	*/
	public void instantiate(ChannelHandler handler) {
        log.trace(this + " building stream for " + handler);
    
		// build channel listener / protocol and set value in the channel handler
		Protocol<T> protocol = instantiateProtocol(handler, protocolInstantiator);

        Assertion.notNull(protocol, "Protocol instantiator returned a null protocol");

		// build protocol listeners, and set values in the protocol
	 	List<ProtocolListener<T>> listeners = instantiateListeners(handler, protocol, this.listenerInstantiators);
        attachListeners(protocol, listeners);

        log.trace(this + " built stream for " + handler);
	}
    
    /**
    * Instantiates the given Protocol<T> using the given handler, and attaches the ChannelListener
    * to the handler
    *
    * Returns the Protocol<T>
    *
    * @throws an AssertionException if the protocol is null
    *
    */
    public static <T> Protocol<T> instantiateProtocol(ChannelHandler handler, ProtocolInstantiator<T> protocolInstantiator) {
        Protocol<T> protocol = null;
        
        try {
            protocol = protocolInstantiator.newInstance(handler);
        } catch (Exception e) {
            log.warn("Error instantiating channel listener for handler: " + handler, e);
        }

        Assertion.notNull(protocol);
        
        handler.listener(protocol);
        
        return protocol;
    }
    
    public static <T> void attachListeners(Protocol<T> target, List<ProtocolListener<T>> listeners) {
        for (ProtocolListener<T> listener : listeners) {
            target.addProtocolListener(listener);
        }
    }
	
	/**
	* For each protocol listener instantiator, call new instance, guardedly, and aggregate the results into a list
	*/
	public static <T> List<ProtocolListener<T>> instantiateListeners(ChannelHandler handler, Protocol<T> protocol, LinkedBlockingQueue<ProtocolListenerInstantiator<T>> instantiators) {
		List<ProtocolListener<T>> listeners = new ArrayList<ProtocolListener<T>>(instantiators.size());
		
		for (ProtocolListenerInstantiator<T> instantiator : instantiators) {
            try {
    			ProtocolListener<T> listener = instantiator.newInstance(handler, protocol);
                if (listener != null) {
                    log.trace(String.format("Instantiated protocol listener for handler/protocol pair -- handler: %s, protocol: %s", handler, protocol));
                    listeners.add(listener);
                }
            } catch (Exception e) {
                log.warn(String.format("Error instantiating protocol listener for handler/protocol pair -- handler: %s, protocol: %s", handler, protocol), e);
            }
		}
		
		return listeners;
	}
    
    public String toString() {
        return String.format("%s --> {%s}", this.protocolInstantiator, this.listenerInstantiatorsStr);
    }
}
