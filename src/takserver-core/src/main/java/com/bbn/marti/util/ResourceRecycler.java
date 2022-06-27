

package com.bbn.marti.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class ResourceRecycler<T> {
	// constructed fields
	private Queue<T> queue;
	private AtomicInteger circulation;
	private AtomicInteger queueSize;
	private AtomicInteger misses;
	private AtomicInteger hits;
	
	// fluent fields
	private Instantiator<T> factory; // responsible for generating new instances
	private Recycler<T> recycler;    // responsible for recycling policy, ie instances are passed back into the queue through this method

	public ResourceRecycler() {
		this.queue = new ConcurrentLinkedQueue<T>();
		this.queueSize = new AtomicInteger(0);		
		this.circulation = new AtomicInteger(0);
		this.misses = new AtomicInteger(0);
		this.hits = new AtomicInteger(0);
	}

	public ResourceRecycler<T> withInstantiator(Instantiator<T> factory) {
		this.factory = factory;
		return this;
	}
	
	public ResourceRecycler<T> withRecycler(Recycler<T> recycler) {
		this.recycler = recycler;
		return this;
	}
	
	private T nextInstance() {
		T result = queue.poll();
		if (result != null) {
			queueSize.decrementAndGet();
		}
		return queue.poll();
	}
	
	private T allocateNew() {
		circulation.incrementAndGet();	
		return factory.instantiate();
	}
	
	public T get() {
		T result = nextInstance();
		
		if (result != null) {
			// have instance we want in the queue - pull out and adjust instrumentation
			hits.getAndIncrement();
		} else {
			// don't have a valid instance -- instantiate one, put it out in the field
			misses.incrementAndGet();			
			result = allocateNew();
		}
		
		return result;
	}
	
	public void give(T in) {
		// clean up for reentry
		T recycled = recycler.recycle(in);
		
		if (queue.offer(recycled)) {
			// made it back into the queue - modify queue size
			queueSize.incrementAndGet();
		} else {
			// queue didn't want it (shouldn't really ever happen) - decrease circulation count
			circulation.decrementAndGet();
		}
	}
	
	public void clean() {
		int circ = circulation.get();
		int slack = queueSize.get();
		int lastMisses = misses.get();
		int lastHits = hits.get();

		if (circ <= 0 || lastHits + lastMisses <= 0) 
			return;

		// POST: slack + circ and hits + misses will both be positive

		int maxRemove = Math.max(slack, 0);
		
		double slackRatio = ((double) slack) / ((double) circ);
		double hitsRatio = ((double) lastHits) / ((double) lastHits + lastMisses);
		
		int toRemove = (int) Math.round(((double) maxRemove) * slackRatio * hitsRatio * .1);
		
	}
}