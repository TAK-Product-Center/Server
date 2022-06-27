

package com.bbn.marti.util.concurrent.future;

/**
* An abstract implementation of a future task that can be triggered synchronously
* (when the future is run), and set asynchronously by the extendor with the 
* setResult/setException methods.
*/
public abstract class AbstractAsyncFutureTask<V> extends DelegateAsyncFuture<V,SettableAsyncFuture<V>> implements AsyncFutureTask<V> {
	protected AbstractAsyncFutureTask() {
		this(SettableAsyncFuture.<V>create());
	}
	
	protected AbstractAsyncFutureTask(SettableAsyncFuture<V> future) {
		// pass future upwards for delegate
		super(future);
	}
	
	public final void run() {
		this.trigger();
	}

	protected abstract void trigger();
	
	protected final SettableAsyncFuture<V> future() {
		return delegate();
	}
	
	protected final void setResult(V value) {
		future().setResult(value);
	}
	
	protected final void setException(Exception thrown) {
		future().setException(thrown);
	}
}