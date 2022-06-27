

package com.bbn.marti.nio.selector;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
* Registers the channel with the selector for the given interest ops.
*/
public class DeregistrationChange extends AbstractSelectorChange {
	private final ChannelHandler handler;

    public DeregistrationChange(
            SettableAsyncFuture<ChannelHandler> future,
            SelectableChannel channel,
            ChannelHandler handler)
    {
        super(future, channel, IOEvent.NONE.flag());
        
        this.handler = handler;
    }
	
    @Override
	public void apply(Selector selector) {
        try {

            channel.keyFor(selector).cancel();
            NetUtils.guardedClose(channel);
            handler.close();

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