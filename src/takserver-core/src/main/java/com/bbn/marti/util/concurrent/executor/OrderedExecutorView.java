

package com.bbn.marti.util.concurrent.executor;

import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
* A view into an asynchronous executor that has the property that 
* all jobs are executed in the order they are received, while sharing
* the underlying thread pool with other views, and optionally imposing limits 
* on the number of pending jobs allowed in the view.
*/
public interface OrderedExecutorView extends AsyncExecutor {
	/**
	* Returns whether there are any pending jobs in the view
	*
	* Intended to be a low-cost facility for determining whether 
	* there are any jobs in the queue, to bypass more costly
	* techniques of inserting a runnable unto a queue and waiting 
	* for it to be called. This use is obviously only helpful if one
	* can guarantee atomicity of the query -> skip/insert self
	* logic.
	*/
	public boolean isEmpty();

	/**
	* When the given AsyncFuture reaches the head of the queue,
	* the executor view halts execution of client jobs until the given 
	* future (used here as a barrier) is triggered.
	*/
	public <V> void barrier(AsyncFuture<V> barrier);
}