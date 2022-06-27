

package com.bbn.marti.util.concurrent.executor;

import java.util.concurrent.atomic.AtomicInteger;

/**
* A concrete Ordered Executor implementation that restricts the total number of 
* pending customer jobs (in the actual executor queue and in the ordered 
* view queues) to a given number. 
*/
public class SizedOrderedExecutor extends SizableOrderedExecutor {
	private final int capacity; // fixed size limit of the core executor
	private final AtomicInteger count; // atomic counter with the total number of jobs in the queue

	public SizedOrderedExecutor(AsyncExecutor delegate, int capacity, String name) {
		super(delegate, capacity, name);
		
		this.capacity = capacity;
		this.count = new AtomicInteger(0);
	}

	/**
	* Increment the count if the result will be at most capacity.
	*
	* return whether we actually incremented
	*/
	protected boolean incrementCount() {
		if (count.get() < this.capacity) {
			// try to get it
			if (count.incrementAndGet() <= this.capacity) {
				// got it
				return true;
			} else {
				// epic fail -- decrement and return false
				count.getAndDecrement();
				return false;
			}
		} else {
			// don't bother
			return false;
		}
	}
	
	protected void decrementCount() {
		count.decrementAndGet();
	}


}