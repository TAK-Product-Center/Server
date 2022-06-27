

package com.bbn.marti.util.concurrent.executor;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.BlockingArrayQueue;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.Tuple;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutureImpl;
import com.bbn.marti.util.concurrent.future.AsyncFutureTask;

/**
* An OrderedExecutor implementation that provides "ordered views" to those who request them
*
* An ordered view is an executor view (of an AsynchronousExecutor) that guarantees that 
* at most one thread will be servicing a job submitted through that view at any one time,
* and that jobs will be executed in the order that they were received (subject to an external
* ordering between any two submit calls: the submit calls are internally atomic in their reasoning,
* but not externally so).
*
*/
public class SizableOrderedExecutor implements OrderedExecutor {
	/**
	* An inner class for creating an ordered view of the outer executor. Size management
	* is enforced by the queue passed into the constructor. The logic of this view is subtle
	* wrt to enforcing counts for the core and view limits, if there are any. Before doing
	* any work, this view checks to see if the core has any room -- if it does, then the
	* view has already reserved a task. Then, the view constructs an AsynchronousFuture
	* and attempts to enqueue it for the view. If there is no room in this queue's view,
	* then the reservation in the core is redacted, and an exception thrown.
	*/
	protected class ClientExecutorView implements OrderedExecutorView {
		/**
		* a runnable that calls schedule next in the outer class
		*
		* intended for use as a tail call that is attached to the end
		* of the currently executing job that lets this view know when 
		* the job has completed, so it can schedule the next job, if there
		* is one
		*/
		private final Runnable scheduleNextJob = new Runnable() {
			public void run() {
				SizableOrderedExecutor.this.unreserve();
				ClientExecutorView.this.scheduleNext();
			}
		};

		private final Queue<Runnable> jobs; // queue of pending jobs. The job currently in execution is at the head of the queue
		private String name = "anon";

		private Runnable updateMetric = new Runnable() {
			public void run() {
				sizeMetric.currentSize.decrementAndGet();
			}
		};
		/**
		* A constructor that allows this view to remain agnostic about 
		* queue limits -- the offer method will return true/false, indicating
		* whether the job was actually accepted
		*/				
		private ClientExecutorView(Queue<Runnable> queue) {
			Assertion.notNull(queue);
			
			this.jobs = queue;
		}
		
		/**
		* general scheme: 
		* - check to see if the core executor has capacity
		* - construct an asynchronous future, add job callback for potentially scheduling job after this one
		* - see if the view queue is empty (there are no running jobs)
		* - if it is empty, submit job to view queue and core queue
		* - otherwise, defer to callback from another job to schedule our submitted job
		*
		* - if the core executor has capacity, but this view does not, then we decrement the outer count upon refusal
		*/
		public <V> AsyncFuture<V> submit(Callable<V> job) {
			// make sure that we can fit the new job in the outer queue
			// will throw an exception if it can't
			SizableOrderedExecutor.this.ensureCapacity();
			
			// build task to encapsulate client job for execution
			AsyncFutureImpl<V> task = AsyncFutureImpl.create(job);

			// submit task to internal queue, add scheduling callback
			scheduleTaskAndAddCallback(task);

			return task;
		}

		public <V> AsyncFuture<V> submit(Runnable job, V result) {
			// make sure that we can fit the new job in the outer queue
			// will throw an exception if it can't
			SizableOrderedExecutor.this.ensureCapacity();
			
			// build task to encapsulate client job for execution
			AsyncFutureImpl<V> task = AsyncFutureImpl.create(job, result);
			
			// submit task to internal queue, add scheduling callback
			scheduleTaskAndAddCallback(task);
			
			return task;
		}
		
		public AsyncFuture<?> submit(Runnable job) {
			// make sure that we can fit the new job in the outer queue
			// will throw an exception if it can't
			SizableOrderedExecutor.this.ensureCapacity();
			
			// build task to encapsulate client job for execution
			AsyncFutureImpl<?> task = AsyncFutureImpl.create(job);
			
			// submit task to internal queue, add scheduling callback
			scheduleTaskAndAddCallback(task);
			
			return task;
		}

		public <V> AsyncFuture<V> submitTask(AsyncFutureTask<V> task) {
			SizableOrderedExecutor.this.ensureCapacity();
			
			scheduleRunnable(task);
			task.addJob(this.scheduleNextJob);
			
			return task;
		}
		
		/**
		* Defer to internal, async future returning submit -- simply not exposed
		* to the client
		*/
		public void execute(Runnable job) {
			submit(job);
		}
		
		public boolean isEmpty() {
			return this.jobs.isEmpty();
		}
		
		/**
		* Submit a future that will block execution until the future returns,
		* without the future being executed/called
		*/
		public <V> void barrier(final AsyncFuture<V> barrier) {
			scheduleRunnable(new Runnable() {
				public void run() {
					barrier.addJob(ClientExecutorView.this.scheduleNextJob);
				}
			});
		}
		
		private void scheduleTaskAndAddCallback(AsyncFutureImpl<?> job) {
			sizeMetric.currentSize.incrementAndGet();

			scheduleRunnable(job);
			
			// add the schedule next job (the job executed when the future has finished executing, that calls
			// back into this object to schedule the next thing)
			job.addJob(this.scheduleNextJob);
//			job.addJob(updateMetric);
		}
		
		private void scheduleRunnable(Runnable job) {

            
			// submit the new job for execution, if the queue was empty
			Tuple<Boolean,Boolean> emptyAndAccepted = getEmptyAndOffer(job);

			boolean empty = emptyAndAccepted.left();			
			boolean accepted = emptyAndAccepted.right();

			if (empty && accepted) {
				// was empty, and this job was accepted into the queue -- execute this job immediately
				SizableOrderedExecutor.this.viewExecute(job);
			} else if (!accepted) {
				// job was not accepted into internal queue -- call outer queue count decrement method
				SizableOrderedExecutor.this.unreserve();
				throw new IllegalStateException("Task rejected from view queue -- not enough room: " + this);
            }
		}

		private void scheduleNext() {
			Runnable nextJob = pollAndGetNext();
			if (nextJob != null) {
				// queue was nonempty after the head 
				// schedule job at the head
				SizableOrderedExecutor.this.viewExecute(nextJob);
			}
		}
		
		/**
		* Synchronized methods for ensuring that only one job is in execution at a time
		*
		* invariant:
		* - the job currently being executed is held at the head of the jobs queue, until it is done,
		* when a callback removes it from the queue and schedules the next pending job, if there is one.
		*
		* scheme:
		* - The submitter atomically checks to see if the queue was empty, and enqueues his task.
		* -- If the queue was empty, there was no job previously in execution, and the new job
		* can unconditionally be executed. Otherwise, the submitter is done.
		* - In both cases, a callback that checks the queue upon job completion is added.
		*
		* - Upon completion, the queue removal/scheduling callback calls the scheduleNext method which
		* atomically removes the job that just finished from the head of the queue (it must be there)
		* and checks to see if there's a pending job behind it. If there is, then it is scheduled: that
		* job must not have already been scheduled, as a submitter would have observed the queue as non
		* empty. If there is not, the queue is now empty, and any submitter will observe this and immediately
		* schedule a job.
		*
		*/
		private synchronized Tuple<Boolean,Boolean> getEmptyAndOffer(Runnable task) {
			boolean wasEmpty = ( jobs.peek() == null );
			boolean accepted = jobs.offer(task);

			return Tuple.create(wasEmpty, accepted);
		}
		
		private synchronized Runnable pollAndGetNext() {
			Runnable head = jobs.poll();
			
			// DEBUG
			Assertion.notNull(head, "Invariant: job at head of queue should be non null -- just finished executing it");
			
			return jobs.peek();
		}
		
		public void name(String name) {
			this.name = name;
		}
		
		public String name() {
			return this.name;
		}
		
		public String toString() {
			return String.format("Ordered Executor View: %s Core: %s", this.name, SizableOrderedExecutor.this);
		}
	}

	// a singleton runnable that calls the on completion method. Intended
	// for notifying extenders that a job has completed.
	private Runnable onCompletionRunnable = new Runnable() {
		public void run() {
			if (coreHasSizeLimit) {
				Assertion.pre(SizableOrderedExecutor.this.coreHasSizeLimit);
				SizableOrderedExecutor.this.unreserve();
			}
			sizeMetric.currentSize.decrementAndGet();
		}
	};
	
	private final static Logger log = Logger.getLogger(SizableOrderedExecutor.class);
	private AsyncExecutor executor; // executor for submitting jobs -- should be unbounded

	private final boolean coreHasSizeLimit; // boolean indicating whether we need to care about updating the core size
	private final int capacity; // maximum number of jobs that can be executing, if applicable, ie. corehasSizeLimit. Otherwise, -1
	private final AtomicInteger count; // atomic count of the number of jobs that are executing, if applicable. Otherwise, null.

	private String name = "anon";
	private final QueueMetric sizeMetric = new QueueMetric();

	protected SizableOrderedExecutor(AsyncExecutor executor, String name) {
		log.info("unlimited size core executor");
		setCoreExecutor(executor);
		
		this.coreHasSizeLimit = false;
		this.capacity = -1;
		this.count = null;
		this.name = name;
	}

	protected SizableOrderedExecutor(AsyncExecutor executor, int sizeLimit, String name) {
		Assertion.pre(sizeLimit >= 0);
		
		if (log.isDebugEnabled()) {
			log.debug("bounded size core executor. sizeLimit: " + sizeLimit);
		}
	
		setCoreExecutor(executor);
		
		this.coreHasSizeLimit = true;
		this.capacity = sizeLimit;
		this.count = new AtomicInteger(0);
		this.name = name;
		sizeMetric.capacity.set(sizeLimit);
	}
	
	private void setCoreExecutor(AsyncExecutor executor) {
		Assertion.notNull(executor);
		this.executor = executor;
	}
	
	/**
	* Simply forward job to executor -- views handle updating the core 
	* size counts themselves
	*/
	private void viewExecute(Runnable viewJob) {
		this.executor.execute(viewJob);
	}

	/**
	* Use own async future feature, as we need to track count/capacity, but 
	* drop async future handle, as the client doesn't want it
	*/
	public void execute(Runnable job) {
		submit(job);
	}
	
	/**
	* Scheme:
	* - check capacity of the queue, reserving a slot if there is one,
	* and throwing an exception to the submitter if there is not
	* - submit job to internal executor, receiving async handle back
	* - attach a completion runnable to the tail end of the job that 
	* frees the slot our runnable had been taking up
	*/
	public <V> AsyncFuture<V> submit(Callable<V> job) {
		ensureCapacity();

		AsyncFuture<V> future = executor.submit(job);
		addCompletionRunnable(future);
		
		return future;
	}
	
	public AsyncFuture<?> submit(Runnable job) {
		ensureCapacity();

		AsyncFuture<?> future = executor.submit(job);
		addCompletionRunnable(future);
		
		return future;
	}
	
	public <V> AsyncFuture<V> submit(Runnable job, V result) {
		ensureCapacity();

		AsyncFuture<V> future = executor.submit(job, result);
		addCompletionRunnable(future);
		
		return future;
	}

	public <V> AsyncFuture<V> submitTask(AsyncFutureTask<V> task) {
		ensureCapacity();
		// POST: exception has been 

		executor.execute((Runnable) task);
		addCompletionRunnable(task);
		
		return task;
	}

	private void addCompletionRunnable(AsyncFuture<?> future) {
		future.addJob(onCompletionRunnable);
	}
	
	/**
	* Reserves an execution slot, if there is one available, and throws an 
	* exception immediately if there is not
	*/
	protected final void ensureCapacity() {
		if (this.coreHasSizeLimit && !reserve()) {
			throw new IllegalStateException("Task couldn't fit in queue of core executor");
		}
	}
	
	/**
	* Attempts to reserve an execution slot, and returns whether one was
	* successfully reserved
	*/
	protected final boolean ensureCapacity_NoThrows() {
		return ( !this.coreHasSizeLimit || reserve() );
	}

	/**
	* Contends for a space iff the current count is less than 
	* the capacity, and 
	*/
	protected final boolean reserve() {
		Assertion.pre(this.coreHasSizeLimit, "Core executor must have size limit for this call to be valid");
		Assertion.pre(this.count.get() >= 0, "Core size count must always be nonnegative");

		if (this.count.get() < this.capacity) {
			// try to get it
			if (this.count.getAndIncrement() < this.capacity) {
				// got it
				return true;
			} else {
				// epic fail -- decrement and return false
				this.count.getAndDecrement();
				return false;
			}
		} else {
			// don't bother trying
			return false;
		}
	}
	
	protected final void unreserve() {
		if (this.coreHasSizeLimit) {
			int queueSize = this.count.getAndDecrement();

			// DEBUG
			Assertion.condition(queueSize > 0, "Queue size must have been positive");
		}
	}
	
	public String name() {
		return this.name;
	}
	
	public void name(String name) {
		this.name = name;
	}
	
	/**
	* Methods for instantiating ordered views of this executor and handing them 
	* to a client (these views are not held internally)
	*
	* The capacity limitation is passed on to a sized queue
	*/
	public OrderedExecutorView createOrderedView() {
		
		Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();
			
		com.bbn.marti.config.Buffer.Queue queueConfig = config.getBuffer().getQueue();
				
		return new ClientExecutorView(new BlockingArrayQueue<>(queueConfig.getQueueSizeInitial(), queueConfig.getQueueSizeIncrement(), queueConfig.getCapacity()));
	}
	
	public OrderedExecutorView createOrderedView(int capacity) {
		
Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();
		
        com.bbn.marti.config.Buffer.Queue queueConfig = config.getBuffer().getQueue();
      		
		return new ClientExecutorView(new BlockingArrayQueue<>(queueConfig.getQueueSizeInitial(), queueConfig.getQueueSizeIncrement(), queueConfig.getCapacity()));
	}
	
	public String toString() {
		return String.format("Ordered Executor Core: %s Pool: %s", this.name, this.executor);
	}
}