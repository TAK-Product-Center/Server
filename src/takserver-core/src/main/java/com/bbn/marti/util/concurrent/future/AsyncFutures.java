

package com.bbn.marti.util.concurrent.future;

import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.executor.AsyncDelegatingExecutor;
import com.bbn.marti.util.concurrent.executor.AsyncExecutor;
import com.google.common.base.Function;

/**
* A set of static asynchronous future utilities for chaining functions, forwarding exceptions,
* and returning identity futures
*/
public class AsyncFutures {
	private static final Logger log = Logger.getLogger(AsyncFutures.class);

	/**
	* Applies a function to the result of the given input future, and returns a future representing
	* the result of that application. If a throwable is generated, either by the input future (its
	* output is EXCEPT and an exception is passed) or by the application of the given function, then 
	* the result of the returned future will be that exception
	*/
	public static <I,O> AsyncFuture<O> transform(AsyncFuture<I> input, Function<I,O> function) {
		return transform(input, function, directSmotheringExecutor());
	}
	
	public static <I,O> AsyncFuture<O> transform(AsyncFuture<I> input, Function<I,O> function, Executor executor) {
		FunctionAdapter<I,O> transform = FunctionAdapter.create(function);
		input.addCallback( (AsyncCallback<I>) transform, executor);
		return (AsyncFuture<O>) transform;
	}

	/**
	* Applies an asynchronous function to the result of the given input future, and returns a future representing 
	* the completion of that asynchronous function.
	*/
	public static <I,O> AsyncFuture<O> transform(AsyncFuture<I> input, AsyncFunction<I,O> function) {
		return transform(input, function, directSmotheringExecutor());
	}

	public static <I,O> AsyncFuture<O> transform(AsyncFuture<I> input, AsyncFunction<I,O> function, Executor executor) {
		AsyncFunctionAdapter<I,O> transform = AsyncFunctionAdapter.create(function);
		input.addCallback( (AsyncCallback<I>) transform, executor);
		return (AsyncFuture<O>) transform;
	}
	
	public static <V> void addCallback(AsyncFuture<V> source, AsyncCallback<V> dest) {
		addCallback(source, dest, directSmotheringExecutor());
	}

	public static <V> void addCallback(AsyncFuture<V> source, AsyncCallback<V> dest, Executor executor) {
		source.addCallback(dest, executor);
	}
	
	public static <V> void forward(AsyncFuture<V> source, SettableAsyncFuture<V> dest) {
		forward(source, dest, directSmotheringExecutor());
	}
	
	public static <V> void forward(AsyncFuture<V> source, SettableAsyncFuture<V> dest, Executor executor) {
		source.addCallback(new AsyncFutureForwarder<V>(dest), executor);
	}

	/**
	* Forwards exceptions that occur on the given input future to the given settable
	*/
	public static <I,O> void forwardExceptions(AsyncFuture<I> input, SettableAsyncFuture<O> output) {
		forwardExceptions(input, output, directSmotheringExecutor());
	}
	
	public static <I,O> void forwardExceptions(AsyncFuture<I> input, final SettableAsyncFuture<O> output, Executor executor) {
		input.addCallback(new AsyncCallback<I>() {
			public void onFailure(Exception t) { output.setException(t); }
			public void onSuccess(I input) {}
		}, executor);
	}
    
	public static <V> AsyncFuture<V> immediateFuture(final V result) {
		if (result != null) {
			// build immediate future to hold the value
			return makeImmediateFuture(result);
		} else {
			// return cast null future -- input was null, doesn't matter
			return (AsyncFuture<V>) immediateNullFuture();
		}
	}
	
	private static <V> AsyncFuture<V> makeImmediateFuture(final V result) {
		return new AbstractDirectExecutorAsyncFuture<V>() {
			public Outcome getStatus() {
				return Outcome.SUCCESS;
			}
			public V getResult() {
				return result;
			}
			public Exception getException() {
				return new IllegalStateException();
			}
			public void addJob(Runnable runnable, Executor executor) {
				executor.execute(runnable);
			}
			public void addCallback(final AsyncCallback<V> callback, Executor executor) {
				executor.execute(new Runnable() {
					public void run() {
						callback.onSuccess(result);
					}
				});
			}
		};		
	}

	public static final AsyncFuture<Object> immediateNullFuture = makeImmediateFuture(null);

	public static AsyncFuture<?> immediateNullFuture() {
		return immediateNullFuture;
	}
	
	public static <V> AsyncFuture<V> immediateFailedFuture(final Exception toThrow) {
		Assertion.notNull(toThrow, "Throwable must be nonnull");
		
		return new AbstractDirectExecutorAsyncFuture<V>() {
			public Outcome getStatus() {
				return Outcome.EXCEPT;
			}
			public V getResult() {
				throw new IllegalStateException();
			}
			public Exception getException() {
				return toThrow;
			}			
			public void addJob(Runnable runnable, Executor executor) {
				executor.execute(runnable);
			}
			public void addCallback(final AsyncCallback<V> callback, Executor executor) {
				executor.execute(new Runnable() {
					public void run() {
						callback.onFailure(toThrow);
					}
				});
			}
		};
	}
    
    public static <V> AsyncCallback<V> loggingCallback(Logger log, String tag) {
        return new ExceptionLoggingAsyncCallback(log, tag);
    }

    public static class ExceptionLoggingAsyncCallback<V> implements AsyncCallback<V> {
        private final Logger log;
        private final String tag;

        public ExceptionLoggingAsyncCallback(Logger log, String tag) {
            this.log = log;
            this.tag = tag;
        }
     
        @Override
        public void onFailure(Exception thrown) {
            log.error("Listening logger received exception in callback: " + tag, thrown);
        }
        
        @Override
        public void onSuccess(V result) {
            ;
        }
    }
    
    public final static AsyncCallback<Object> prettyPrintingAsyncCallback = new AsyncCallback<Object>() {
        @Override
        public void onFailure(Exception thrown) {
            System.out.println(this + " received exception in async callback: " + thrown);
        }
        
        @Override
        public void onSuccess(Object result) {
            System.out.println(this + " received success in async callback: " + result);
        }
    };

	public static final Executor directSmotheringExecutor = new Executor() {
		public void execute(Runnable job) {
			Assertion.notNull(job, "Executor job submitted was null");
			
			try {
				job.run();
			} catch (Exception e) {
				log.error("Exception while trying to execute runnable in direct executor", e);
			}
		}
	};
    
    public static final AsyncExecutor directSmotheringAsyncExecutor = new AsyncDelegatingExecutor(directSmotheringExecutor, "directSmothering");
	
	public static AsyncExecutor directSmotheringExecutor() {
		return exceptionSmotheringDirectExecutor();
	}
	
	public static AsyncExecutor exceptionSmotheringDirectExecutor() {
		return directSmotheringAsyncExecutor;
	}
    
    public static boolean guardedExecute(Executor executor, Runnable job) {
        try {
            executor.execute(job);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}