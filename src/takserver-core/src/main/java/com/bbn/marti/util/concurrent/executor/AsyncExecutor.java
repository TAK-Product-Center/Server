

package com.bbn.marti.util.concurrent.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutureTask;

/**
* An interface for an executor service like executor that returns asynchronous
* futures as a result of each job submission.
*
*/
public interface AsyncExecutor extends Executor {
	/**
	* Returns, asynchronously, the result of calling into the callable
	*/
	public <V> AsyncFuture<V> submit(Callable<V> callable);
	
	/**
	* Returns, asynchronously, the untyped, null (null is every type) result of executing the given runnable
	*
	* Intended for use in establishing simple happens-before relationships between jobs
	*/
	public AsyncFuture<?> submit(Runnable runnable);
	
	/**
	* Returns the given result when the runnable has finished execution
	*/
	public <V> AsyncFuture<V> submit(Runnable runnable, V result);
	
	/**
	* Schedules the given task to have "run" called on it, and returns the 
	* given async future, without the task subtype exposed
	*
	* The task is not done when the runnable finishes executing, but 
	* when the actual future is complete (the executor itself subscribes
	* to the future's listener roll)
	*/
	public <V> AsyncFuture<V> submitTask(AsyncFutureTask<V> task);
	
	public String name();
	public void name(String name);
}
