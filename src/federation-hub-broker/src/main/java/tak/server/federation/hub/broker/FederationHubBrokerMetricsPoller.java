package tak.server.federation.hub.broker;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubResources;

public class FederationHubBrokerMetricsPoller implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerMetricsPoller.class);

	private final ScheduledFuture<?> scheduleFuture;

    public FederationHubBrokerMetricsPoller() {
        logger.info("Initializing federation hub broker metrics poller.");

        // scheduleAtFixedRate will run every x seconds no matter what
        // scheduleWithFixedDelay will run every x seconds starting AFTER the logic has completed
        scheduleFuture = FederationHubResources.metricsScheduler.scheduleWithFixedDelay(() -> {

            FederationHubBrokerMetrics latestBrokerMetrics = FederationHubDependencyInjectionProxy.getInstance()
    				.federationHubBrokerMetrics();
            
            Counter writtenCounter = Metrics.counter("total.messages.written");
            writtenCounter.increment(latestBrokerMetrics.getTotalWrites());
        
            Counter readCounter = Metrics.counter("total.messages.read");
            readCounter.increment(latestBrokerMetrics.getTotalReads());
            
            Counter totalMessagesDroppedCounter = Metrics.counter("total.messages.dropped");
            totalMessagesDroppedCounter.increment(latestBrokerMetrics.getTotalMessagesDropped());
             
            // latency metrics
            Counter totalLatencyMs = Metrics.counter("average.latency.ms");
            totalLatencyMs.increment(latestBrokerMetrics.averageLatencyMs());
            
            Counter totalLatencyNs = Metrics.counter("average.latency.ns");
            totalLatencyNs.increment(latestBrokerMetrics.getAverageLatencyNs());
            
            FederationHubDependencyInjectionProxy.getInstance().federationHubBrokerMetrics().resetLatency();
		}, 1, FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfigManager().getConfig().getCloudwatchStepSeconds(), TimeUnit.SECONDS);

    }

    @Override
    public void destroy() throws Exception {
        // to stop
        scheduleFuture.cancel(true);
    }
}