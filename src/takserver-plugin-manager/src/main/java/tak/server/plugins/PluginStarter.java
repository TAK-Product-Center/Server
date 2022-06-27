package tak.server.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;

import com.google.common.base.Strings;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import tak.server.CommonConstants;
import tak.server.messaging.Messenger;


public class PluginStarter {

	private final String natsURL;
	private final String natsClusterId;

	private Connection natsConnection;
	private Dispatcher dispatcher;

	private Map<String, MessageReceiver> receiverPlugins;
	private Map<String, MessageSender> senderPlugins;
	private Map<String, MessageSenderReceiver> senderReceiverPlugins;
	private Map<String, MessageInterceptor> interceptorPlugins;

	@Autowired
	GenericApplicationContext context;

	@Autowired
	Ignite ignite;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private Messenger<Message> pluginMessenger;

	private static final Logger logger = LoggerFactory.getLogger(PluginStarter.class);

	private final ExecutorService starterPool;
	
	private final ForkJoinPool interceptorSendPool = newForkJoinPool("plugin-interceptor-send-worker");
	private final ForkJoinPool interceptorProcessPool = newForkJoinPool("plugin-interceptor-process-worker");

	public PluginStarter(String natsURL, String natsClusterId) {
		starterPool = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
		this.natsURL = natsURL;
		this.natsClusterId = natsClusterId;
	}

	@EventListener(PluginsLoadedEvent.class)
	private void init() {

		try {
			initSenderPlugins();
			initReceiverPlugins();
			initSenderReceiverPlugins();
			initInterceptorPlugins();

			if (Strings.isNullOrEmpty(natsClusterId)) {
				initIgniteListener();
			} else {
				initNatsListener();
			}
		} catch (Exception e) {
			logger.error("error inititalizing plugins", e);
		}
		
		// get spring to set properties now
		applicationEventPublisher.publishEvent(new PluginsStartedEvent(this, "plugins started"));
	}

	private void submitBytesToReceivers(byte[] rawMessage) {
		try {

			Message message = Message.parseFrom(rawMessage);

			receiverPlugins.forEach((name, receiver) -> {
				if (receiver.getPluginInfo().isEnabled()) {
					receiver.onMessage(message);
				}
			});

			senderReceiverPlugins.forEach((name, senderReceiver) -> {
				if (senderReceiver.getPluginInfo().isEnabled()) {
					senderReceiver.onMessage(message);
				}
			});

			interceptorSendPool.submit(() -> {
				try {

					Iterator<MessageInterceptor> it = interceptorPlugins.values().iterator();

					CompletableFuture<Message> f = null;

					while (it.hasNext()) {

						final MessageInterceptor plug = it.next();

						if (f == null) {
							f = CompletableFuture.supplyAsync(() -> plug.intercept(message), interceptorProcessPool);
						} else {
							f = f.thenCompose(m -> CompletableFuture.supplyAsync(() -> plug.intercept(m), interceptorProcessPool));
						}
					}

					if (f != null) {

						final Messenger<Message> fpm = pluginMessenger;

						try {
							
							// block, get result
							Message processedMessage = f.get();
							
						    Message.Builder mb = processedMessage.toBuilder();
		                    
		                    // add default provenance to guard against loops
		                    mb.addProvenance("PluginManager");
		                    
							fpm.send(mb.build());
						} catch (InterruptedException | ExecutionException e) {
							logger.warn("interupted plugin send " + message);
						}
					}
				} catch (Exception e) {
					logger.warn("exception processing intercept plugin", e);
				}
			});

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception deserializing plugin message", e);
			}
		}
	}

	private void initNatsListener() {
		try {
			natsConnection = Nats.connect(natsURL);

			// message dispatcher
			dispatcher = natsConnection.createDispatcher();

			dispatcher.subscribe(CommonConstants.CLUSTER_PLUGIN_SUBSCRIBE_TOPIC, CommonConstants.CLUSTER_PLUGIN_SUBSCRIBE_GROUP, m ->
			submitBytesToReceivers((byte[]) (byte[]) m.getData()));
		} catch (Exception e) {
			logger.error("exception connecting to NATS server to receive messages", e);
		}
	}

	private void initIgniteListener() {
		ignite.message().localListen(CommonConstants.PLUGIN_SUBSCRIBE_TOPIC, (nodeId, rawMessage) -> {

			if (logger.isTraceEnabled()) {
				logger.trace("received message on plugin subscribe topic " + CommonConstants.PLUGIN_SUBSCRIBE_TOPIC);
			}

			if (!(rawMessage instanceof byte[])) {

				if (logger.isDebugEnabled()) {
					logger.debug("ignoring unsupported message type " + rawMessage.getClass().getName());
				}

				// return true to continue listening
				return true;
			}

			submitBytesToReceivers((byte[]) rawMessage);

			// return true to continue listening
			return true;
		});
	}

	private void initSenderReceiverPlugins() {
		senderReceiverPlugins = context.getBeansOfType(MessageSenderReceiver.class);

		AtomicInteger senderReceiverCount = new AtomicInteger();

		senderReceiverPlugins.forEach((name, senderReceiver) -> {
			starterPool.execute(() -> {
				senderReceiver.start();

				if (logger.isDebugEnabled()) {
					logger.debug("started sender-receiver plugin " + name + " " + senderReceiver.getClass().getName());
				}

				senderReceiverCount.incrementAndGet();
			});
		});
	}

	private void initReceiverPlugins() {
		receiverPlugins = context.getBeansOfType(MessageReceiver.class);

		AtomicInteger receiverCount = new AtomicInteger();

		receiverPlugins.forEach((name, receiver) -> {
			starterPool.execute(() -> {
				receiver.start();

				if (logger.isDebugEnabled()) {
					logger.debug("started receiver plugin " + name + " " + receiver.getClass().getName());
				}

				receiverCount.incrementAndGet();
			});
		});
	}

	private void initSenderPlugins() {
		senderPlugins = context.getBeansOfType(MessageSender.class);

		AtomicInteger senderCount = new AtomicInteger();

		senderPlugins.forEach((name, sender) -> {
			starterPool.execute(() -> {
				sender.start();

				if (logger.isDebugEnabled()) {
					logger.debug("started sender plugin " + name + " " + sender.getClass().getName());
				}

				senderCount.incrementAndGet();
			});
		});
	}
	
	private void initInterceptorPlugins() {
		interceptorPlugins = context.getBeansOfType(MessageInterceptor.class);

		AtomicInteger interceptorCount = new AtomicInteger();
		
		// track any registration of interceptor plugins so that the messaging process submission service can behave accordingly
		if (!interceptorPlugins.isEmpty()) {
			ignite.atomicLong(PluginManagerConstants.INTERCEPTOR_REGISTRATION_KEY, 1, true);
		} else {
			ignite.atomicLong(PluginManagerConstants.INTERCEPTOR_REGISTRATION_KEY, 0, true);
		}

		interceptorPlugins.forEach((name, interceptor) -> {
			starterPool.execute(() -> {
				interceptor.start();

				if (logger.isDebugEnabled()) {
					logger.debug("started interceptor plugin " + name + " " + interceptor.getClass().getName());
				}

				interceptorCount.incrementAndGet();
			});
		});
	}

	public Collection<MessageReceiver> getReceiverPlugins() {
		return receiverPlugins.values();
	}

	public Collection<MessageSender> getSenderPlugins() {
		return senderPlugins.values();
	}

	public Collection<MessageSenderReceiver> getSenderReceiverPlugins() {
		return senderReceiverPlugins.values();
	}

	public Collection<PluginLifecycle> getAllPlugins() {
		Collection<PluginLifecycle> plugins = new ArrayList<>();
		plugins.addAll(getReceiverPlugins());
		plugins.addAll(getSenderPlugins());
		plugins.addAll(getSenderReceiverPlugins());

		return plugins;
	}
	
	private static class TakServerExceptionHandler implements Thread.UncaughtExceptionHandler {

		Logger logger = LoggerFactory.getLogger(TakServerExceptionHandler.class);

		@Override
		public void uncaughtException(Thread thread, Throwable t) {
			logger.error("Uncaught exception", t);
		}
	}

    private ForkJoinWorkerThreadFactory newWorkerFactory(final String baseName) {
    	return new ForkJoinWorkerThreadFactory() {
          @Override           
          public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
              final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
              worker.setName(baseName + "-" + worker.getPoolIndex());
              return worker;
          }
    		
    	};
    }

    private ForkJoinPool newForkJoinPool(String poolWorkerbaseName) {
    	return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), newWorkerFactory(poolWorkerbaseName), new TakServerExceptionHandler(), false); 
    }
}
