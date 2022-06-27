

package com.bbn.marti.util.concurrent.future;

/**
* An Asynchronous callback that forwards its received results through to another future
*
* Intended for use in lightweight "dereferencing" of nested async future types
*/
public class AsyncFutureForwarder<V> implements AsyncCallback<V> {
	private final SettableAsyncFuture<V> future;
	
	protected AsyncFutureForwarder(SettableAsyncFuture<V> future) {
		this.future = future;
	}
	
	public final void onFailure(Exception thrown) {
		future.setException(thrown);
	}
	
	public final void onSuccess(V result) {
		future.setResult(result);
	}
}