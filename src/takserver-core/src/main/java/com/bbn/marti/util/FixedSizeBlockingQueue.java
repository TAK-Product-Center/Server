

package com.bbn.marti.util;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Buffer.Queue;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.service.DistributedConfiguration;

public class FixedSizeBlockingQueue<E> {
	
	private static final Logger logger = LoggerFactory.getLogger(FixedSizeBlockingQueue.class);
	
	public QueueMetric queueMetric = new QueueMetric();
	private final BlockingQueue<E> queue;

	public FixedSizeBlockingQueue() {
		
		Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();
		
		Queue queueConfig = config.getBuffer().getQueue();
		
     	queue = new BlockingArrayQueue<>(queueConfig.getQueueSizeInitial(), queueConfig.getQueueSizeIncrement(), queueConfig.getCapacity());
     	queueMetric.capacity.set(queueConfig.getCapacity());
	}

	public boolean add(E element) {
		if (element != null) {
			try {
				queue.add(element);
				queueMetric.currentSize.getAndIncrement();
			} catch (IllegalStateException e) {
				// queue full
				if (logger.isDebugEnabled()) {
					logger.debug("FixedSizeBlockingQueue full");
				}

				return false;
			}
		}
		
		// null insertion not allowed
		return false;
	}

	// block
	public E take() throws InterruptedException {
		E toReturn = queue.take();
		queueMetric.currentSize.decrementAndGet();
		return toReturn;
	}
	
	// don't block, return null if empty
	public E poll() throws InterruptedException {
		E element = queue.poll();
		
		if (element != null) {
			queueMetric.currentSize.decrementAndGet();
		}
		
		return element;
	}

	public QueueMetric getQueueMetrics() {
		return queueMetric;
	}
}
