

package com.bbn.marti.nio.binder.impls;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.server.ChannelWrapper;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;

/**
* An abstract binder class for holding the stream/pipeline instantiators,
* and the local server port
*/
public abstract class AbstractServerBinder implements ServerBinder {
    // bind address
    private int port = -1;
	private InetSocketAddress bindAddress;

    // stream builder
	private StreamInstantiator streamInstantiator;

    // executor handed to the emitted object
    private OrderedExecutor boundExecutor;

    // main call that all subclass need to implement
    @Override
    public abstract ChannelWrapper handleBind(Server server) throws IOException;

    /*   set calls   */
	public final AbstractServerBinder withPort(int port) {
		Assertion.condition(MessageConversionUtil.isValidPort(port));
        return withBindAddress(new InetSocketAddress(port));
	}
	
	public final AbstractServerBinder withBindAddress(InetSocketAddress bindAddress) {
		Assertion.notNull(bindAddress);
		Assertion.isNull(this.bindAddress);
        Assertion.pre(this.port == -1);
		
		this.bindAddress = bindAddress;
        this.port = bindAddress.getPort();
        
		return this;
	}
	
	public final AbstractServerBinder withInstantiator(StreamInstantiator instantiator) {
		Assertion.notNull(instantiator);
		Assertion.isNull(this.streamInstantiator);
		
		this.streamInstantiator = instantiator;
		return this;
	}

    public final AbstractServerBinder withBoundExecutor(OrderedExecutor executor) {
        Assertion.notNull(executor);
        
        this.boundExecutor = executor;
        return this;
    }
		
    /*   return calls  */
	protected final InetSocketAddress bindAddress() {
		Assertion.notNull(bindAddress);
		
		return this.bindAddress;
	}
	
	protected final StreamInstantiator instantiator() {
		Assertion.notNull(streamInstantiator, "Stream builder never set for this binder");
		
		return this.streamInstantiator;
	}
    
    protected final int port() {
        Assertion.condition(this.port != -1, "Port never set for this binder");
        
        return this.port;
    }

    protected final OrderedExecutor boundExecutor() {
        Assertion.notNull(this.boundExecutor != null, "Io processor never set for this binderr");

        return this.boundExecutor;
    }
}    
