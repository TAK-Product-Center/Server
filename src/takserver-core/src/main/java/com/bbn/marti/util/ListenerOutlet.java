

package com.bbn.marti.util;

/**
* A simple class for handling submitting fan-outs to multiple Consumers
*/
public interface ListenerOutlet<E> {
	// consumer set modifier methods
	public boolean add(Consumer<E> consumer);
	public boolean remove(Consumer<E> consumer);
	public boolean contains(Consumer<E> consumer);

	// consumer notification methods
	public boolean broadcast(E input);
}