

package com.bbn.marti.util.concurrent.future;

/**
* An interface for subscribing to the results of an asynchronous future
*/
public interface AsyncCallback<V> {
    /**
    * Called when an exception is encountered during the execution of an 
    * asynchronous job, or when the result of the future is explicitly
    * set to be exceptional
    */
	public void onFailure(Exception t);
    
    /**
    * Called when the execution of an asynchronous job completes
    * with the given result, being returned from execution, provided
    * beforehand and set after completion, or set asynchronously
    */
	public void onSuccess(V result);
}