

package com.bbn.marti.util.concurrent.future;

import java.util.concurrent.Executor;

/**
* An asynchronous future implementation that delegates all calls in the 
* AsyncFuture interface to a contained, typed implementation of AsyncFuture.
*
* Allows an extender to "mimic" select behavior of another asynchronous future without having
* to carbon copy the code to tie all the calls through, while extending and augmenting the
* behavior of other calls.
*
* The extender adds a type argument for the concrete AsyncFuture implementation and passes a
* concrete object to this super, allowing the delegation methods to treat the future as
* an interface, and for the extender to treat it as the concrete
*/
public abstract class DelegateAsyncFuture<V,E extends AsyncFuture<V>> implements AsyncFuture<V> {
	private final E delegate;
	
	protected DelegateAsyncFuture(E delegate) {
		this.delegate = delegate;
	}
	
	protected final E delegate() {
		return this.delegate;
	}
	
	public Outcome getStatus() {
		return delegate.getStatus();
	}

	public V getResult() {
		return delegate.getResult();
	}

	public Exception getException() {
		return delegate.getException();
	}

	public void addJob(Runnable runnable) {
		delegate.addJob(runnable);
	}
	
	public void addJob(Runnable runnable, Executor executor) {
		delegate.addJob(runnable, executor);
	}
	
	public void addCallback(AsyncCallback<V> callback) {
		delegate.addCallback(callback);
	}
	
	public void addCallback(AsyncCallback<V> callback, Executor executor) {
		delegate.addCallback(callback, executor);
	}
}