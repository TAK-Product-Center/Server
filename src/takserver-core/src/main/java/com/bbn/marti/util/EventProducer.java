

package com.bbn.marti.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* A class for publishing a generic event to a set of consumers of type EventConsumer.
*
* <p> Uses ReadWriteLocks for synchronizing on the consumer set, so that multiple events can be posted 
* at one time. 
*
* TODO: check that hashsets are safe for rwlocks, ie, get/contains methods don't change any internal organization.
*
*/
public class EventProducer<E> {
	private Set<EventConsumer<E>> consumers;
	private ReadWriteLock rwlock;
	
	public EventProducer() {
		this.consumers = new HashSet<EventConsumer<E>>();
		this.rwlock = new ReentrantReadWriteLock(true); // flag for fairness
	}
	
	private Lock writeLock() {
		return this.rwlock.writeLock();
	}
	
	private Lock readLock() {
		return this.rwlock.readLock();
	}
	
	public boolean register(EventConsumer<E> consumer) {
		boolean result = false;
		
		Lock wrlock = writeLock();
		wrlock.lock();
		
		try {
			// deal with equal hashing case, already contains case
			result = consumers.add(consumer);
		} catch (Exception e) {
			// TODO: log warn
		} finally {
			wrlock.unlock();
		}
		
		return result;
	}
	
	public boolean isRegistered(EventConsumer<E> consumer) {
		boolean result = false;
		
		Lock rlock = readLock();
		rlock.lock();
		
		try {
			if (consumers.contains(consumer))
				result = true;
		} catch (Exception e) {
			// TODO: log warn
		} finally {
			rlock.unlock();
		}
		
		return result;
	}
	
	public boolean deregister(EventConsumer<E> consumer) {
		boolean result = false;
		
		Lock wrlock = writeLock();
		wrlock.lock();

		try {
			result = consumers.remove(consumer);
		} catch (Exception e) {
			// TODO: log warn
		} finally {
			wrlock.unlock();
		}
		
		return result;
	}
	
	
	public void broadcast(E input) {
		Lock rlock = readLock();
		rlock.lock();

		try {
			for (EventConsumer<E> consumer : consumers) {
				consumer.notify(input);
			}
		} catch (Exception e) {
			// TODO: log warn
		} finally {
			rlock.unlock();
		}
	}

	public Runnable broadcastTask(E input) {
		return new CallBroadcastTask<E>()
			.withProducer(this)
			.withEvent(input);
	}

	private static class CallBroadcastTask<E> implements Runnable {
		private EventProducer<E> producer;
		private E input;
		
		public CallBroadcastTask<E> withProducer(EventProducer<E> producer) {
			this.producer = producer;
			return this;
		}
		
		public CallBroadcastTask<E> withEvent(E input) {
			this.input = input;
			return this;
		}
		
		public void run() {
			this.producer.broadcast(this.input);
		}
	}
}