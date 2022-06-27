

package com.bbn.marti.util.concurrent.future;

import java.util.concurrent.Executor;

/**
* An abstract asynchronous future class that handles specification of a direct executor to the
* add_ with a specified executor
*/
public abstract class AbstractDirectExecutorAsyncFuture<V> implements AsyncFuture<V> {
	public final void addJob(Runnable runnable) {
		addJob(runnable, AsyncFutures.exceptionSmotheringDirectExecutor());
	}
	public final void addCallback(AsyncCallback<V> callback) {
		addCallback(callback, AsyncFutures.exceptionSmotheringDirectExecutor());
	}
}