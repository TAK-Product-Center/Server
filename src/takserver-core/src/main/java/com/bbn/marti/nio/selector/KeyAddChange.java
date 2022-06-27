

package com.bbn.marti.nio.selector;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* Adds the requested IO event to the Interest OP bit vector. If already present, the subscription remains.
*/
public class KeyAddChange extends AbstractSelectorChange {
    public KeyAddChange(
        SettableAsyncFuture<ChannelHandler> future,
        SelectableChannel channel,
        int interest) 
    {
        super(future, channel, interest);
    }
        
	public void doApply(SelectionKey key) {
        int newInterest = IOEvent.addInterest(key.interestOps(), interest);
        
        // set the key
        key.interestOps(newInterest);
        
        setFuture(key);
	}
}