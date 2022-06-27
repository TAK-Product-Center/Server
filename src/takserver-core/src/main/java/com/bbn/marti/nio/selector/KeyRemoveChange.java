

package com.bbn.marti.nio.selector;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* Removes the requested IO Event from the Interest Op bit vector. If the event is not present,
* this operation has no effect.
*/
public class KeyRemoveChange extends AbstractSelectorChange {
    public KeyRemoveChange(
        SettableAsyncFuture<ChannelHandler> future,
        SelectableChannel channel,
        int interest) 
    {
        super(future, channel, interest);
    }

	public void doApply(SelectionKey key) {
        // remove interest from current key
        int newInterest = IOEvent.removeInterest(key.interestOps(), interest);
        
        // set into key
		key.interestOps(newInterest);
	}
}