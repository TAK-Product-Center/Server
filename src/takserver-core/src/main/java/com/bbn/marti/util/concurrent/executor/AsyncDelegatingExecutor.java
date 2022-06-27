

package com.bbn.marti.util.concurrent.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.util.concurrent.future.AsyncCallback;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutureImpl;
import com.bbn.marti.util.concurrent.future.AsyncFutureTask;

/**
* An asynchronous executor that relies, internally, on a given executor to execute runnable
* objects constructed to wrap client tasks and asynchronously call chained tasks when completed.
*/
public class AsyncDelegatingExecutor implements AsyncExecutor {

	private final Executor delegate;
	private String name = "anon_delegate";
	private final QueueMetric currentSize = new QueueMetric();

	public AsyncDelegatingExecutor(Executor delegate, String name) {
		this.delegate = delegate;
		this.name = name;
	}
	
	public void execute(Runnable runnable) {
		delegate.execute(runnable);
	}

	public <V> AsyncFuture<V> submit(Callable<V> callable) {
		AsyncFutureImpl<V> task = AsyncFutureImpl.create(callable);
		return submitAndReturn(task);
	}

	public <V> AsyncFuture<V> submit(Runnable runnable, V result) {
		AsyncFutureImpl<V> task = AsyncFutureImpl.create(runnable, result);
		return submitAndReturn(task);
	}
		
	public AsyncFuture<?> submit(Runnable runnable) {
		AsyncFutureImpl<?> task = AsyncFutureImpl.create(runnable);
		return submitAndReturn(task);
	}

	public <V> AsyncFuture<V> submitTask(AsyncFutureTask<V> task) {
		currentSize.currentSize.incrementAndGet();
		task.addCallback(new AsyncCallback<V> () {
							 @Override
							 public void onFailure(Exception t) {
								 currentSize.currentSize.decrementAndGet();
							 }
							 @Override
							 public void onSuccess(V result) {
								 currentSize.currentSize.decrementAndGet();
							 }
						 }
		);
		delegate.execute((Runnable) task);
		return task;
	}
	
	private <V> AsyncFuture<V> submitAndReturn(AsyncFutureImpl<V> task) {
		currentSize.currentSize.incrementAndGet();
		task.addCallback(new AsyncCallback<V> () {
							 @Override
							 public void onFailure(Exception t) {
								 currentSize.currentSize.decrementAndGet();
							 }
							 @Override
							 public void onSuccess(V result) {
								 currentSize.currentSize.decrementAndGet();
							 }
						 }
		);
		delegate.execute((Runnable) task);
		return task;
	}
	
	public String name() {
		return name;
	}
	
	public void name(String name) {
		this.name = name;
	}

}