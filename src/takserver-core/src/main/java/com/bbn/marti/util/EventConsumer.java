

package com.bbn.marti.util;

/**
* An interface for subscribing to an EventProducer.
*/
public interface EventConsumer<E> {
	public void notify(E event);
}