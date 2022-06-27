

package com.bbn.marti.util;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class AsyncRWLocksListenerOutlet<E> extends RWLocksListenerOutlet<E> {
	ListeningExecutorService broadcastExecutor;
	
	private static class ConsumerBroadcastTask<E> implements Callable<Boolean> {
		private final Set<Consumer<E>> consumers;	
		private final E toBroadcast;
		private final ReadWriteLock rwlock;
		
		public ConsumerBroadcastTask(Set<Consumer<E>> consumers, E input, ReadWriteLock lock) {
			this.consumers = consumers;
			this.toBroadcast = input;
			this.rwlock = lock;
		}
		
		public Boolean call() {
			return RWLocksListenerOutlet.broadcast(consumers, toBroadcast, rwlock);
		}
	}

	public ListenableFuture<Boolean> asyncBroadcast(E input) {
		Callable<Boolean> broadcastTask = new ConsumerBroadcastTask<E>(consumers(), input, lock());
		return broadcastExecutor.submit(broadcastTask);
	}
	
	public AsyncRWLocksListenerOutlet<E> withExecutorService(ListeningExecutorService executor) {
		this.broadcastExecutor = executor;
		return this;
	}
}