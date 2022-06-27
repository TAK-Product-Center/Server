

package com.bbn.marti.util.concurrent.future;

public interface AsyncFunction<I,O> {
	public AsyncFuture<O> apply(I input);
}