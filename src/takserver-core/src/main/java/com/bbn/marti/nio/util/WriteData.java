

package com.bbn.marti.nio.util;

import java.nio.ByteBuffer;

import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* An object encapsulating a single write coming in from the pipeline/marti
*/
public class WriteData {
	public final ByteBuffer data;
	public final int originalLength;
	public final SettableAsyncFuture<Integer> future;

	public WriteData(ByteBuffer data, SettableAsyncFuture<Integer> writeFuture) {
		this.data = data;
		this.originalLength = data.remaining();
		this.future = writeFuture;
	}
    
    public WriteData(ByteBuffer data) {
        this(data, SettableAsyncFuture.<Integer>create());
    }

    public void setFuture() {
        if (future != null)
            future.setResult(originalLength);
    }

    public void setException(Exception err) {
        if (future != null)
            future.setException(err);
    }
}