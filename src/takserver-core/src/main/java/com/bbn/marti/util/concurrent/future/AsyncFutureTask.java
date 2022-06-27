

package com.bbn.marti.util.concurrent.future;

public interface AsyncFutureTask<V> extends AsyncFuture<V>, Runnable {
	public void run();
}