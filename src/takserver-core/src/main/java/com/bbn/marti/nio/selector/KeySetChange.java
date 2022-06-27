

package com.bbn.marti.nio.selector;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* Sets the interest op bit vector to that given. Blows away the preceeding interest op bit vector.
*/
public class KeySetChange extends AbstractSelectorChange {
    public KeySetChange(
        SettableAsyncFuture<ChannelHandler> future,
        SelectableChannel channel,
        int interest) 
    {
        super(future, channel, interest);
    }
    
	public void doApply(SelectionKey key) {
		key.interestOps(this.interest);
	}
}