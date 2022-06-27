

package com.bbn.marti.util.concurrent.future;

/**
* An asynchronous future for representing the result of applying a function to the
* outcome of (yet) another asynchronous future.
*
* If the source future results in a failure, the throwable is passed to all listeners,
* and the function is ignored. In the case of a chain of functions chained together
* on top of an asynchronous future, an exception at any point will propagate through
* all the futures, and received by a callback listener at the tail.
*
*/
public class AsyncFunctionAdapter<I,O> extends DelegateAsyncFuture<O,SettableAsyncFuture<O>> implements AsyncCallback<I> {
	private final AsyncFunction<I,O> function;
	
	protected AsyncFunctionAdapter(SettableAsyncFuture<O> outputFuture, AsyncFunction<I,O> function) {
		super(outputFuture);
		this.function = function;
	}
	
	public static <I,O> AsyncFunctionAdapter<I,O> create(AsyncFunction<I,O> function) {
		SettableAsyncFuture<O> future = SettableAsyncFuture.create();
		return new AsyncFunctionAdapter<I,O>(future, function);
	}
	
	public void onFailure(Exception t) {
		delegate().setException(t);
	}
	
	public void onSuccess(I result) {
		try {
			// when the original function returns, we forward the output to the thing we returned originally (this settable)
			AsyncFuture<O> asyncFunResult = this.function.apply(result);

			AsyncCallback<O> callback = new AsyncFutureForwarder<O>(delegate());
			asyncFunResult.addCallback(callback);
		} catch (Exception t) {
			this.onFailure(t);
		}
	}
}