

package com.bbn.marti.nio.selector;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* Abstract base class for applying a registration or interest op change to a selector. Handles exceptional
* and successful results from applying the change, and forwards these to the settable future. The future
* should *always* be set as a result of applying any change, no matter what the outcome.
*
* Has a few methods for retrieving and auto-casting the attachment/scheduler from a selection key.
*
*/
public abstract class AbstractSelectorChange {
    private final static Logger log = Logger.getLogger(AbstractSelectorChange.class);
    private final static IllegalStateException unregisteredOrInvalidKeyException = new IllegalStateException("Channel is not registered with the selector, or will be imminently cancelled");

	public final SettableAsyncFuture<ChannelHandler> future;
	public final SelectableChannel channel;
	public final int interest;
    
    protected AbstractSelectorChange(
        SettableAsyncFuture<ChannelHandler> future,
        SelectableChannel channel,
        int interest)
    {
        this.future = future;
        this.channel = channel;
        this.interest = interest;
    }

	/**
	* Delegate apply method -- either returns the channel handler if the change can be applied without
	* exception, or throws an exception if it cannot be. We pass the exception on to the change future.
	*/
	public void apply(Selector selector) {
        // retrieve key for channel from this selector
        SelectionKey key = NetUtils.validSelectionKeyOrNull(this.channel, selector);
        if (key != null) {
            // have valid/active registration -- do operation
            try {
                doApply(key);
                
                // POST: operation was successful -- set the future
                setFuture(key);
            } catch (Exception e) {
                // operation was unsuccessful -- warn and except the future
                log.error("Error encountered applying selection change (severe)", e);
                exceptFuture(e);
            }
        } else {
            // channel doesn't have registration -- except the future
            exceptFuture(unregisteredOrInvalidKeyException);
        }
    }

    public abstract void doApply(SelectionKey key) throws IOException;

    protected final void setFuture(SelectionKey key) {
        setFuture((ChannelHandler) key.attachment());
    }

    protected final void setFuture(ChannelHandler handler) {
        future.setResult(handler);
    }

    protected final void exceptFuture(Exception thrown) {
        future.setException(thrown);
    }
}