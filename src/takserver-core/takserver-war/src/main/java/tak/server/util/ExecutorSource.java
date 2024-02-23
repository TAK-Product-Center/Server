/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package tak.server.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bbn.marti.config.Queue;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.CoreConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ExecutorSource {
	
	private final Configuration config;

	private final Queue queue;

	private final int POOL_SIZE_INITIAL = 1;

	private final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();

	private final int DEFAULT_POOL_MAX = NUM_AVAIL_CORES;

	private final int POOL_SIZE_MAX;

	public final int EXEC_QUEUE_SIZE;
	
	public final boolean IS_LOW_CORE; // forceLowConcurrency option in config can be used to set low concurrency mode
	
	public final ExecutorService missionRepositoryProcessor;
	
	public ExecutorSource() {
		
		config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		queue = config.getBuffer().getQueue();

		POOL_SIZE_MAX = DEFAULT_POOL_MAX < queue.getDefaultMaxPoolSize() ? queue.getDefaultMaxPoolSize() : DEFAULT_POOL_MAX;

		EXEC_QUEUE_SIZE = config.getBuffer().getQueue().getDefaultExecQueueSize() * NUM_AVAIL_CORES;
		
		IS_LOW_CORE = NUM_AVAIL_CORES < 4 || config.isForceLowConcurrency(); // forceLowConcurrency option in config can be used to set low concurrency mode
		
		missionRepositoryProcessor = newExecutorService("MissionRepository",  POOL_SIZE_INITIAL, POOL_SIZE_MAX * 2);

	}
	
	private ExecutorService newExecutorService(String name, int initialPoolSize, int maxPoolSize) {

		return newExecutorService(name, initialPoolSize, maxPoolSize, EXEC_QUEUE_SIZE);
	}

	private static ExecutorService newExecutorService(String name, int initialPoolSize, int maxPoolSize, int queueSize) {

		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.build();

		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueSize);
		return new TakServerThreadPoolExecutor(initialPoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, workQueue, threadFactory);
	}

	@SuppressWarnings("unused")
	private static ScheduledExecutorService newScheduledExecutor(String name, int size) {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.build();

		return new ScheduledThreadPoolExecutor(size, threadFactory);
	}

	private ThreadPoolTaskExecutor websocketExecutor() {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat("takserver-socket-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.build();
		
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(POOL_SIZE_INITIAL);
		taskExecutor.setMaxPoolSize(POOL_SIZE_MAX);
		taskExecutor.setQueueCapacity(EXEC_QUEUE_SIZE);
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setKeepAliveSeconds(120);
		taskExecutor.setThreadFactory(threadFactory);

		return taskExecutor;
	}

	private static class TakServerThreadPoolExecutor extends ThreadPoolExecutor {

		TakServerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		}

		Logger logger = LoggerFactory.getLogger(TakServerThreadPoolExecutor.class);

		public TakServerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		@Override
		public void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			// If submit() method is called instead of execute()
			if (t == null && r instanceof Future<?>) {
				try {
					((Future<?>) r).get();
				} catch (CancellationException e) {
					t = e;
				} catch (ExecutionException e) {
					t = e.getCause();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (t != null) {
				// Exception occurred

				logger.error("Uncaught exception ", t);
			}
			// can perform cleanup actions here
		}
	}

	private static class TakServerExceptionHandler implements Thread.UncaughtExceptionHandler {

		Logger logger = LoggerFactory.getLogger(TakServerExceptionHandler.class);

		@Override
		public void uncaughtException(Thread thread, Throwable t) {
			logger.error("Uncaught exception", t);
		}
	}
}

