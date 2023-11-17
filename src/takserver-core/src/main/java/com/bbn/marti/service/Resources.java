

package com.bbn.marti.service;

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

import com.bbn.marti.config.Buffer.Queue;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.util.concurrent.executor.AsyncDelegatingExecutor;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.executor.SizedOrderedExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class Resources {

	private static Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();

	private static Queue queue = DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue();

	private static final int POOL_SIZE_INITIAL = 1;

	private static final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();

	private static final int DEFAULT_POOL_MAX = NUM_AVAIL_CORES;

	private static final int POOL_SIZE_MAX = DEFAULT_POOL_MAX < queue.getDefaultMaxPoolSize() ? queue.getDefaultMaxPoolSize() : DEFAULT_POOL_MAX;

	public static final int EXEC_QUEUE_SIZE = config.getBuffer().getQueue().getDefaultExecQueueSize() * NUM_AVAIL_CORES;
	
	public static final boolean IS_LOW_CORE = NUM_AVAIL_CORES < 4 || config.isForceLowConcurrency(); // forceLowConcurrency option in config can be used to set low concurrency mode
	
	public static final OrderedExecutor lowCoreOrderedExecutor;
	public static final ExecutorService lowCoreExecutorService;
	public static final ScheduledExecutorService lowCoreScheduledExecutorService;
	public static final ExecutorService lowCoreGrpcExecutorService;
	public static final EventLoopGroup lowCoreGrpcEventLoopGroup;
	
	// create a minimal set of executors if low core mode is enabled
	static {
		if (IS_LOW_CORE) {
			lowCoreOrderedExecutor = newOrderedExecutor("takserver-ordered", queue.getCoreExecutorCapacity(), POOL_SIZE_INITIAL, DEFAULT_POOL_MAX);
			lowCoreExecutorService = newExecutorService("takserver", POOL_SIZE_INITIAL, DEFAULT_POOL_MAX);
			lowCoreScheduledExecutorService = newScheduledExecutor("takserver-scheduled", DEFAULT_POOL_MAX);
			lowCoreGrpcExecutorService = newGrpcThreadPoolExecutor("takserver-grpc", POOL_SIZE_INITIAL, DEFAULT_POOL_MAX);
			lowCoreGrpcEventLoopGroup = newGrpcEventLoopGroup("takserver-grpc-eventgroup", DEFAULT_POOL_MAX);
		} else {
			lowCoreOrderedExecutor = null;
			lowCoreExecutorService = null;
			lowCoreScheduledExecutorService = null;
			lowCoreGrpcExecutorService = null;
			lowCoreGrpcEventLoopGroup = null;
		}
	}
	
	// pools that are always used
	// mission repository always get its own pool
	// larger pool sizes to accomodate blocking database query operations
	public static final ExecutorService missionRepositoryProcessor = newExecutorService("MissionRepository",  POOL_SIZE_INITIAL, POOL_SIZE_MAX * 2);
	public static final ExecutorService missionContentProcessor = newExecutorService("MissionContentProcessor",  POOL_SIZE_INITIAL, POOL_SIZE_MAX * 2);
	public static final ExecutorService messagePersistenceProcessor = newExecutorService("MessagePersistence",  POOL_SIZE_INITIAL, POOL_SIZE_MAX * 2);
	public static final ExecutorService callsignAuditExecutor = newExecutorService("CallsignAuditPersistenceExecutor", POOL_SIZE_INITIAL, POOL_SIZE_MAX * 2);
	
	// other pools
	public static final ExecutorService qosCacheProcessor = !IS_LOW_CORE ? newExecutorService("QoSCacheExectuor",  POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;
	
	// pool used for handling accept/connect operations
	public static final OrderedExecutor acceptAndConnectProcessor = !IS_LOW_CORE ? newOrderedExecutor("AcceptConnect", queue.getCoreExecutorCapacity(), POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreOrderedExecutor;

	// pool used for handling read/write operations
	public static final OrderedExecutor tcpProcessor = !IS_LOW_CORE ? newOrderedExecutor("TcpProcessor", queue.getCoreExecutorCapacity(), POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreOrderedExecutor;

	// pool used for handling publishing of state information to cluster
	public static final ExecutorService clusterStateProcessor = !IS_LOW_CORE ? newExecutorService("ClusterStateProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;
	
	public static final ExecutorService clusterMissionStateProcessor = !IS_LOW_CORE ? newExecutorService("ClusterMissionStateProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for handling publishing of state information to cluster
	public static final ScheduledExecutorService scheduledClusterStateExecutor = !IS_LOW_CORE ? newScheduledExecutor("ScheduleClusterStateExecutor", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	// pool used for copying messages
	public static final ExecutorService messageCopyProcessor = !IS_LOW_CORE ? newExecutorService("MessageCopyProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for processing messages
	public static final ExecutorService messageProcessor = !IS_LOW_CORE ? newExecutorService("MessageProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	public static final ExecutorService readParseProcessor = !IS_LOW_CORE ? newExecutorService("ReadParseProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;
	
	public static final ExecutorService writeParseProcessor = !IS_LOW_CORE ? newExecutorService("WriteParseProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;
	
	public static final ExecutorService tcpStaticSubProcessor = !IS_LOW_CORE ? newExecutorService("TcpStaticSubProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for proto negotiation
	public static final ExecutorService negotiationProcessor = !IS_LOW_CORE ? newExecutorService("NegotiationProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for handling udp read/write operations
	public static final OrderedExecutor udpProcessor = !IS_LOW_CORE ? newOrderedExecutor("UdpProcessor", queue.getCoreExecutorCapacity(), POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreOrderedExecutor;

	// pool used for handling message injection.
	public static final OrderedExecutor injectionProcessor = !IS_LOW_CORE ? newOrderedExecutor("InjectionProcessor", EXEC_QUEUE_SIZE, POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreOrderedExecutor;

	// pool used for message brokering
	public static final ExecutorService brokerProcessor = !IS_LOW_CORE ? newExecutorService("BrokerProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for determining message destinations
	public static final ExecutorService brokerDestinationsProcessor = !IS_LOW_CORE ? newExecutorService("BrokerDestinationsProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for determining src / dest matching
	public static final ExecutorService brokerMatchingProcessor = !IS_LOW_CORE ? newExecutorService("BrokerMatchingProcessor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool used for message brokering
	public static final ExecutorService fedMissionPackageExecutor = !IS_LOW_CORE ? newExecutorService("FedMissionPackageExecutor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;
	
	// use a single pool here to help keep ROL submissions ordered. This is important for resource creates + updates where order matters
	public static final ExecutorService federationROLExecutor = newExecutorService("FederationROLExecutor", 1, 1);
	
	// pool used for periodic auth updates
	public static final ScheduledExecutorService authThreadPool = !IS_LOW_CORE ? newScheduledExecutor("AuthUpdateProcessor", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	// pool used for closing tcp connections
	public static final ScheduledExecutorService tcpCloseThreadPool = !IS_LOW_CORE ? newScheduledExecutor("TcpCloseProcessor", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	public static final ScheduledExecutorService flushPool = !IS_LOW_CORE ? newScheduledExecutor("FlushPool", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	// pool for reconnecting federates
	public static final ScheduledExecutorService fedReconnectThreadPool = !IS_LOW_CORE ? newScheduledExecutor("FedReconnectPool", 1) : lowCoreScheduledExecutorService;

	// pool for testing database connection
	public static final ScheduledExecutorService dbHealthThreadPool = !IS_LOW_CORE ? newScheduledExecutor("DBHealthPool", 1) : lowCoreScheduledExecutorService;

	// pool for repeaters and federate health check messages
	public static final ScheduledExecutorService repeaterPool = !IS_LOW_CORE ? newScheduledExecutor("RepeaterPool", 1) : lowCoreScheduledExecutorService;

	// pool for repeaters and federate health check messages
	public static final ScheduledExecutorService metricsReportingPool = !IS_LOW_CORE ? newScheduledExecutor("MetricsReportingPool", 1) : lowCoreScheduledExecutorService;

	// pool for running the ghost connection cleanup check
	public static final ScheduledExecutorService ghostConnectionCleanupPool = !IS_LOW_CORE ? newScheduledExecutor("GhostConnectionCleanupPool", 1) : lowCoreScheduledExecutorService;

	// pool for listener removal
	public static ExecutorService removeListenerPool = !IS_LOW_CORE ? newExecutorService("removeListenerPool", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool for listener removal
	public static ScheduledExecutorService removeProtoListenerPool = !IS_LOW_CORE ? newScheduledExecutor("removeProtoListenerPool", POOL_SIZE_MAX) : lowCoreScheduledExecutorService;

	// pool for listener addition
	public static ExecutorService addListenerPool = !IS_LOW_CORE ? newExecutorService("addListenerPool", 1, 1) : lowCoreExecutorService;

	// pool used for processing group information. Using a single thread for now.
	public static final OrderedExecutor groupProcessor = !IS_LOW_CORE ? newOrderedExecutor("GroupProcessor", EXEC_QUEUE_SIZE, 1, 1) : lowCoreOrderedExecutor;

	// pool used for x509-ldap group lookup
	public static final ExecutorService x509ldapProcessor = !IS_LOW_CORE ? newExecutorService("x509ldap", 1, 5) : lowCoreExecutorService;

	public static final ExecutorService messageCacheProcessor = !IS_LOW_CORE ? newExecutorService("MessageCache",  POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// pool for store/forward of missed chat messages, handles the db query and submission of missed messages
	public static final ExecutorService storeForwardChatDbExecutor = newExecutorService("storeForwardChatDbExecutor", POOL_SIZE_INITIAL, POOL_SIZE_MAX);

	// pool for store/forward of missed chat messages, handles the processing of db results and submission of messages
	public static final ScheduledExecutorService storeForwardChatSendExecutor = newScheduledExecutor("storeForwardChatSendExecutor", POOL_SIZE_MAX);

	// single threaded executor used by the broker service for ordered deliver of store/forward messages
	public static final ExecutorService storeForwardChatProcessor = newExecutorService("storeForwardChatProcessor", 1, 1);

	// pool for persisting callsign audit activity
	public static final ExecutorService callsignAssignmentExecutor = !IS_LOW_CORE ? newExecutorService("CallsignAssignmentExecutor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// producer-consumer pool for message writes. This pool will be busy under load.
	public static final ExecutorService messageWritePoolExecutor = !IS_LOW_CORE ? newExecutorService("messageWritePoolExecutor", 1, 1, queue.getMessageWriteExecutorQueueSize()) : lowCoreExecutorService;

	public static final ExecutorService messageSendExecutor = !IS_LOW_CORE ? newExecutorService("MessageSendExecutor", POOL_SIZE_INITIAL, POOL_SIZE_MAX) : lowCoreExecutorService;

	// Bounded Executor pool for federation grpc server and channel builders
	public static final ExecutorService federationGrpcExecutor = !IS_LOW_CORE ? newGrpcThreadPoolExecutor("grpc-federation-executor", POOL_SIZE_INITIAL, NUM_AVAIL_CORES) : lowCoreGrpcExecutorService;

	// Bounded worker pool for federation grpc server and channel builders
	public static final EventLoopGroup federationGrpcWorkerEventLoopGroup = !IS_LOW_CORE ? newGrpcEventLoopGroup("grpc-federation-worker", NUM_AVAIL_CORES) : lowCoreGrpcEventLoopGroup;

	// Bounded Executor pool for  grpc input server and channel builders
	public static final ExecutorService grpcInputExecutor = !IS_LOW_CORE ? newGrpcThreadPoolExecutor("grpc-input-executor", POOL_SIZE_INITIAL, NUM_AVAIL_CORES) : lowCoreGrpcExecutorService;

	// Bounded worker pool for grpc input server and channel builders
	public static final EventLoopGroup grpcInputWorkerEventLoopGroup = !IS_LOW_CORE ? newGrpcEventLoopGroup("grpc-input-worker", NUM_AVAIL_CORES) : lowCoreGrpcEventLoopGroup;

	private static OrderedExecutor newOrderedExecutor(String name, int capacity, int initialSize, int maxSize) {
	
		ExecutorService executor = newExecutorService(name, initialSize, maxSize);

		return new SizedOrderedExecutor(new AsyncDelegatingExecutor(executor, name), capacity, name);
	}

	private static ExecutorService newExecutorService(String name, int initialPoolSize, int maxPoolSize) {

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

	private static ScheduledExecutorService newScheduledExecutor(String name, int size) {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.build();

		return new ScheduledThreadPoolExecutor(size, threadFactory);
	}

	public static ThreadPoolTaskExecutor websocketExecutor() {
		
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

	private static EventLoopGroup newGrpcEventLoopGroup(String name, int maxPoolSize) {
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat(name + "-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.setDaemon(true)
				.build();
		
		return new NioEventLoopGroup(maxPoolSize, threadFactory);
	}

	private static ExecutorService newGrpcThreadPoolExecutor(String name, int initialPoolSize, int maxPoolSize) {
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(EXEC_QUEUE_SIZE);
		
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
				.setNameFormat("websocket-%1$d")
				.setUncaughtExceptionHandler(new TakServerExceptionHandler())
				.build();
		
		
		return new ThreadPoolExecutor(initialPoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS, workQueue, threadFactory);
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

