

package com.bbn.marti.util.concurrent.future;

import java.util.concurrent.Executor;

import com.bbn.marti.util.Assertion.AssertionException;

/**
* An interface for monitoring and attaching jobs/events to the completion of 
* some encapsulated, asynchronous computation.
*
* This type of future does not extends Java's future: the thread blocking methods
* are difficult to implement cheaply, and expensive to implement easily, and (should) 
* see little use in an entirely event driven system.
*
* Inspired by Google Guava's handy, but expensive ListenableFuture construct
*
*/
public interface AsyncFuture<V> {
	/**
	* Enum indicating the outcome of this future
	*
	* The scheduled/executing boundaries are not guaranteed to be 
	* completely up to date. 
	*
	* If an outcome is in {SUCCESS, EXCEPT}, then 
	* getResult/getException may be called without fear
	* of exceptions 
	*/
	public enum Outcome {
		SCHEDULED, EXECUTING, CANCELLED, SUCCESS, EXCEPT;
	}

	/**
	* Returns the outcome of this outcome. It is only valid to
	* make this call if the job/state of this future has completed
	*/
	public Outcome getStatus();

	/**
	* Returns the typed result of this future. It is only valid to make
	* this call if the outcome is SUCCESS
	*
	* @return the typed result of the async future
	* @throws AssertionException if the future did not finish with a SUCCESS outcome
	*/
	public V getResult();

	/**
	* Returns the exceptional result of this future. It is only valid
	* to make this call if the outcome is EXCEPT
	*
	* @return the throwable that resulted from attempting to complete the asynchronous
	* computation
	* @throws AssertionException if the future did not finish with an EXCEPT outcome
	*/
	public Exception getException();

	/**
	* Adds a job to be executed after the completion of this async future. If the
	* job is already complete, the job will run on the current thread. Otherwise,
	* the job will be executed on the same thread that was processing this future.
	*/
	public void addJob(Runnable runnable);
	
	/**
	* Adds a job to be executed after the completion of this async future. Job
	* is scheduled to run on the given executor. The job will be scheduled for 
	* execution on the given executor when the job completes, or will be scheduled
	* immediately if the job is already complete.
	*/
	public void addJob(Runnable runnable, Executor executor);
	
	/**
	* Adds a callback that implements an onSuccess(V) or an onFailure(Throwable) 
	* interface, allowing computation to continue based on the result of this future.
	*
	* The execution semantics are similar to those of addJob
	*/
	public void addCallback(AsyncCallback<V> callback);
	
	public void addCallback(AsyncCallback<V> callback, Executor executor);
}