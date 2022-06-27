

package com.bbn.marti.util.concurrent.future;

import com.google.common.base.Function;

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
public class FunctionAdapter<I,O> extends DelegateAsyncFuture<O,SettableAsyncFuture<O>> implements AsyncCallback<I> {
	private final Function<I,O> function;
	
	protected FunctionAdapter(SettableAsyncFuture<O> outputFuture, Function<I,O> function) {
		super(outputFuture);
		
		this.function = function;
	}
	
	protected static <I,O> FunctionAdapter<I,O> create(Function<I,O> function) {
		SettableAsyncFuture<O> future = SettableAsyncFuture.create();
		return new FunctionAdapter<I,O>(future, function);
	}
	
	public final void onFailure(Exception thrown) {
		delegate().setException(thrown);
	}
	
	public final void onSuccess(I result) {
		try {
			O funResult = this.function.apply(result);
			delegate().setResult(funResult); // it *was* fun
		} catch (Exception thrown) {
			delegate().setException(thrown);
		}
	}
}