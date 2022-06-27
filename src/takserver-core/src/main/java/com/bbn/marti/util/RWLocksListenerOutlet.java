

package com.bbn.marti.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWLocksListenerOutlet<E> implements ListenerOutlet<E> {
	private final ReadWriteLock rwlock;
	private final Set<Consumer<E>> consumers;
	
	public RWLocksListenerOutlet() {
		rwlock = new ReentrantReadWriteLock(true);
		consumers = new HashSet<Consumer<E>>();
	}

	protected ReadWriteLock lock() {
		return rwlock;
	}
	
	protected Set<Consumer<E>> consumers() {
		return consumers;
	}
	
	// consumer set modifier methods
	public boolean add(Consumer<E> consumer) {
		Lock wrlock = lock().writeLock();
		wrlock.lock();

		boolean result = false;
		try {
			result = consumers.add(consumer);
		} catch (Exception e) {
			// log warn;
		}

		wrlock.unlock();
		return result;
	}
	
	public boolean remove(Consumer<E> consumer) {
		Lock wrlock = lock().writeLock();
		wrlock.lock();

		boolean result = false;
		try {
			result = consumers.remove(consumer);
		} catch (Exception e) {
			// log warn;
		}

		wrlock.unlock();
		return result;
	}
	
	public boolean contains(Consumer<E> consumer) {
		Lock rdlock = lock().readLock();
		rdlock.lock();

		boolean result = false;
		try {
			result = consumers.contains(consumer);
		} catch (Exception e) {
			// log warn;
		}
		
		rdlock.unlock();
		return result;
	}

	// consumer notification methods
	public boolean broadcast(E input) {
		return broadcast(consumers, input, lock());
	}

	protected static <T> boolean broadcast(Set<Consumer<T>> consumers, T input, ReadWriteLock lock) {
		Lock rdlock = lock.readLock();
		rdlock.lock();

		boolean allWant = allWant(input, consumers);

		if (allWant) {
			allWant = broadcastAll(input, consumers);
		}
		
		rdlock.unlock();
		return allWant;
	}

	protected static <T> boolean allWant(T input, Collection<Consumer<T>> lockedConsumers) {
		boolean allWant = true;
		
		for (Consumer<T> consumer : lockedConsumers) {
			try {
				if (!consumer.wants(input)) {
					allWant = false;
					break;
				} 
			} catch (Exception e) {
				// log warn
				allWant = false;
				break;
			}
		}
		
		return allWant;
	}

	protected static <T> boolean broadcastAll(T input, Collection<Consumer<T>> lockedConsumers) {
		boolean allAccepted = true;

		for (Consumer<T> consumer : lockedConsumers) {
			try {
				if (!consumer.submit(input) && allAccepted) {
					allAccepted = false;
				}
			} catch (Exception e) {
				allAccepted = false;
			}
		}

		return allAccepted;
	}
}