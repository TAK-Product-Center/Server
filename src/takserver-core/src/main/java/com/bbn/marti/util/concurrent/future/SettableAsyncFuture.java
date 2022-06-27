

package com.bbn.marti.util.concurrent.future;

import com.bbn.marti.util.Assertion;
import com.google.common.base.Optional;

/**
* An asynchronous future that can be set with a direct call to setResult/setException instead of
* by executing a runnable. This extender blocks out the runnable method of the super class, and
* adds a synchronization layer to the setResult/setException pair (in the super class, all synchronization
* is around the receiver list reference, and it is assumed that the job will only be run and completed
* once).
*
*/
public class SettableAsyncFuture<V> extends AsyncFutureImpl<V> {
	protected SettableAsyncFuture() {
		super();
	}

	/**
	* static instantiator for inferring the destination type
	*/
	public static <V> SettableAsyncFuture<V> create() {
		return new SettableAsyncFuture<V>();
	}
				
	/**
	* Prevent the super class from ever being executed.
	*/
	@Override
	public final void run() {
		Assertion.fail();
	}


	protected final synchronized ListenerNode getHeadAndSetResult(Optional<V> result) {
		ListenerNode head = getHeadAndVoid();
		
		if (head != null) {
			this.outcomeStatus = Outcome.SUCCESS;
			this.successResult = result;
		}
		
		return head;
	}
	
	protected final synchronized ListenerNode getHeadAndSetException(Exception thrown) {
		ListenerNode head = getHeadAndVoid();

		if (head != null) {
			this.outcomeStatus = Outcome.EXCEPT;
			this.failureResult = thrown;
		}

		return head;
	}

	/**
	* Set the state of the super class to success/except, and notify
	* all receivers
	*/
	@Override
	public final boolean setResult(V result) {
		ListenerNode head = super.receiverHead;

		// double-checked head is null
		if (head != null &&
			(head = getHeadAndSetResult(Optional.fromNullable(result))) != null) {
			super.notifyReceivers(head);
		}
		
		return (head != null);
	}
	
	@Override
	public final boolean setException(Exception thrown) {
		ListenerNode head = super.receiverHead;
		
		// double-checked head is null
		if (head != null &&
			(head = getHeadAndSetException(thrown)) != null) {
			super.notifyReceivers(head);
		}
		
		return (head != null);
	}
}