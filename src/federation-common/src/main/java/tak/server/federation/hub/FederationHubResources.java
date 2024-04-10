package tak.server.federation.hub;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class FederationHubResources {
	private static final int POOL_SIZE_INITIAL = 1;

	private static final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();

	private static final int DEFAULT_POOL_MAX = NUM_AVAIL_CORES;

	private static final int POOL_SIZE_MAX = DEFAULT_POOL_MAX < 8 ? 8 : DEFAULT_POOL_MAX;

	public static final int EXEC_QUEUE_SIZE = 1024 * NUM_AVAIL_CORES;
	
	public static final boolean IS_LOW_CORE = NUM_AVAIL_CORES < 4;
	
	public static final ExecutorService lowCoreExecutorService;
	public static final ScheduledExecutorService lowCoreScheduledExecutorService;
	public static final ExecutorService lowCoreGrpcExecutorService;
	public static final EventLoopGroup lowCoreGrpcEventLoopGroup;
	
	// create a minimal set of executors if low core mode is enabled
	static {
		if (IS_LOW_CORE) {
			lowCoreExecutorService = newExecutorService("federation-hub", POOL_SIZE_INITIAL, DEFAULT_POOL_MAX);
			lowCoreScheduledExecutorService = newScheduledExecutor("federation-hub-scheduled", DEFAULT_POOL_MAX);
			lowCoreGrpcExecutorService = newGrpcThreadPoolExecutor("federation-hub-grpc", POOL_SIZE_INITIAL, DEFAULT_POOL_MAX);
			lowCoreGrpcEventLoopGroup = newGrpcEventLoopGroup("federation-hub-grpc-eventgroup", DEFAULT_POOL_MAX);
		} else {
			lowCoreExecutorService = null;
			lowCoreScheduledExecutorService = null;
			lowCoreGrpcExecutorService = null;
			lowCoreGrpcEventLoopGroup = null;
		}
	}
	
	public static final ExecutorService rolExecutor = !IS_LOW_CORE ? newGrpcThreadPoolExecutor("rol-federation-hub-executor", POOL_SIZE_INITIAL, NUM_AVAIL_CORES) : lowCoreGrpcExecutorService;
	
	public static final ScheduledExecutorService mfdtScheduler = !IS_LOW_CORE ? newScheduledExecutor("mfdt-federation-hub-scheduler", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	public static final ScheduledExecutorService healthCheckScheduler = !IS_LOW_CORE ? newScheduledExecutor("health-check-federation-hub-scheduler", 1) : lowCoreScheduledExecutorService;

	public static final ScheduledExecutorService retryScheduler = !IS_LOW_CORE ? newScheduledExecutor("outgoing-connection-federation-hub-scheduler", 1) : lowCoreScheduledExecutorService;
	
	public static final ScheduledExecutorService dbRetentionScheduler = !IS_LOW_CORE ? newScheduledExecutor("db-retention-federation-hub-scheduler", 1) : lowCoreScheduledExecutorService;

	public static final ScheduledExecutorService metricsScheduler = !IS_LOW_CORE ? newScheduledExecutor("metrics-federation-hub-scheduler", 1) : lowCoreScheduledExecutorService;

	// Bounded Executor pool for federation grpc server and channel builders
	public static final ExecutorService federationGrpcExecutor = !IS_LOW_CORE ? newGrpcThreadPoolExecutor("grpc-federation-hub-executor", POOL_SIZE_INITIAL, NUM_AVAIL_CORES) : lowCoreGrpcExecutorService;

	// Bounded worker pool for federation grpc server and channel builders
	public static final EventLoopGroup federationGrpcWorkerEventLoopGroup = !IS_LOW_CORE ? newGrpcEventLoopGroup("grpc-federation-hub-worker", NUM_AVAIL_CORES) : lowCoreGrpcEventLoopGroup;

	
	public static ExecutorService newExecutorService(String name, int initialPoolSize, int maxPoolSize) {

		return newExecutorService(name, initialPoolSize, maxPoolSize, EXEC_QUEUE_SIZE);
	}

	private static ExecutorService newExecutorService(String name, int initialPoolSize, int maxPoolSize, int queueSize) {

		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new FederationHubExceptionHandler())
				.build();

		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueSize);
		return new FederationHubThreadPoolExecutor(initialPoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, workQueue, threadFactory);
	}

	private static ScheduledExecutorService newScheduledExecutor(String name, int size) {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new FederationHubExceptionHandler())
				.build();

		return new ScheduledThreadPoolExecutor(size, threadFactory);
	}

	public static ThreadPoolTaskExecutor websocketExecutor() {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat("federation-hub-socket-%1$d")
				.setUncaughtExceptionHandler(new FederationHubExceptionHandler())
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

	private static EventLoopGroup newGrpcEventLoopGroup(String name, int maxPoolSize) {
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new FederationHubExceptionHandler())
				.setDaemon(true)
				.build();
		
		if (Epoll.isAvailable()) {
			return new EpollEventLoopGroup(maxPoolSize, threadFactory);
		} else {
			return new NioEventLoopGroup(maxPoolSize, threadFactory);	
		}		
	}

	private static ExecutorService newGrpcThreadPoolExecutor(String name, int initialPoolSize, int maxPoolSize) {
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(EXEC_QUEUE_SIZE);
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat("websocket-%1$d")
				.setUncaughtExceptionHandler(new FederationHubExceptionHandler())
				.build();
		
		
		return new ThreadPoolExecutor(initialPoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS, workQueue, threadFactory);
	}

	private static class FederationHubThreadPoolExecutor extends ThreadPoolExecutor {

		FederationHubThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		}

		Logger logger = LoggerFactory.getLogger(FederationHubThreadPoolExecutor.class);

		public FederationHubThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
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

	private static class FederationHubExceptionHandler implements Thread.UncaughtExceptionHandler {

		Logger logger = LoggerFactory.getLogger(FederationHubExceptionHandler.class);

		@Override
		public void uncaughtException(Thread thread, Throwable t) {
			logger.error("Uncaught exception", t);
		}
	}
}
