package tak.server.federation.hub.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.DisposableBean;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.FederationHubBrokerMetrics;
import tak.server.federation.hub.broker.FederationHubBrokerMetrics.ChannelInfo;

public class FederationHubBrokerMetricsPoller implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerMetricsPoller.class);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private FederationHubBroker fedHubBroker;

	private final ScheduledFuture<?> scheduleFuture;

    public FederationHubBrokerMetricsPoller(FederationHubBroker fedHubBroker) {
    	this.fedHubBroker = fedHubBroker;
        logger.info("Initializing federation hub broker metrics poller.");

        // scheduleAtFixedRate will run every x seconds no matter what
        // scheduleWithFixedDelay will run every x seconds starting AFTER the logic has completed
        scheduleFuture = scheduler.scheduleWithFixedDelay(() -> {

            FederationHubBrokerMetrics latestBrokerMetrics = fedHubBroker.getFederationHubBrokerMetrics();

            ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> channelWriteCounters = latestBrokerMetrics.getChannelInfosInternal();
            for (Map.Entry<String, ConcurrentHashMap<String, ChannelInfo>> senderEntries : channelWriteCounters.entrySet()) {
                String source = senderEntries.getKey();
                ConcurrentHashMap<String, ChannelInfo> receiverCounters = senderEntries.getValue();
                for (String target : receiverCounters.keySet()) {
                    long writtenCount = receiverCounters.get(target).messagesWritten;
                    ArrayList<Tag> tags = new ArrayList<>();
                    tags.add(Tag.of("source", source));
                    tags.add(Tag.of("target", target));
                    Counter counter = Metrics.counter("messages.written", tags);
                    counter.increment(writtenCount - counter.count());
                }
            }


		}, 1, 1, TimeUnit.SECONDS);

    }

    @Override
    public void destroy() throws Exception {
        // to stop
        scheduleFuture.cancel(true);
    }
}