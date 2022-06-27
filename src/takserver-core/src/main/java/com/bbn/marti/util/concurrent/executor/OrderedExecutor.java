

package com.bbn.marti.util.concurrent.executor;

/**
* An interface for an executor that can create executor views that guarantee
* the execution ordering of submitted jobs: specifically, that with respect
* to a single ordered view, at most one thread will be servicing the submitted
* jobs at a given time, and that jobs will be completed in submitted order.
*
* Implementors should ensure that the core OrderedExecutor does not hold
* a reference to an ordered view, so that views can be garbage collected
* when dropped by the client.
*
*/
public interface OrderedExecutor extends AsyncExecutor {
	/**
	* Returns an unlimited (depends on the underlying queue implementation,
	* but Integer.MAX_VALUE is typical) OrderedExecutorView that relies on the 
	* underlying thread pool for execution.
	*
	* If the underlying OrderedExecutor is itself limited in size,
	* than the practical size limit of the view will be unstable 
	* and dependent on usage of the core executor, but will 
	* never be more than the limit of the core.
	*/
	public OrderedExecutorView createOrderedView();

	/**
	* Returns a size-limited view of the ordered executor
	*/
	public OrderedExecutorView createOrderedView(int capacity);
}