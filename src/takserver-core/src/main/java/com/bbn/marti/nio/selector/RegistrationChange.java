

package com.bbn.marti.nio.selector;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* Registers the channel with the selector for the given interest ops.
*/
public class RegistrationChange extends AbstractSelectorChange {
	private final ChannelHandler handler;
	
    public RegistrationChange(
        SettableAsyncFuture<ChannelHandler> future,
        SelectableChannel channel,
        int interest,
        ChannelHandler handler)
    {
        super(future, channel, interest);
        
        this.handler = handler;
    }
	
    @Override
	public void apply(Selector selector) {
        try {
            channel.register(
                selector,
                interest,
                handler
            );

            // POST: successfully registered the channel -- set the future
            setFuture(handler);
        } catch (Exception e) {
            exceptFuture(e);
        }
	}

    @Override
    public void doApply(SelectionKey key) throws IOException {
        Assertion.fail("Should never be called for this change type");
    }
}