package tak.server.federation.hub.broker;


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;

import com.google.common.util.concurrent.AtomicDouble;

public class ActuatorMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(ActuatorMetricsService.class);
    private AtomicInteger cpuCount = new AtomicInteger();
    private AtomicDouble heapUsed = new AtomicDouble();
    private AtomicDouble heapCommitted = new AtomicDouble();
    private AtomicDouble cpuUsed = new AtomicDouble();
    private MetricsEndpoint metricsEndpoint;

    public ActuatorMetricsService(MetricsEndpoint metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
    }

    public AtomicDouble getHeapUsed() {

        MetricsEndpoint.MetricDescriptor heapUsedMetricDescriptor = metricsEndpoint.metric("jvm.memory.used", Arrays.asList("area:heap"));
        if (heapUsedMetricDescriptor != null) {
            heapUsed.set(heapUsedMetricDescriptor
                    .getMeasurements()
                    .get(0)
                    .getValue());
        } else {
            heapUsed.set(-1);
        }

        return heapUsed;
    }

    public AtomicDouble getHeapCommitted() {

        MetricsEndpoint.MetricDescriptor heapCommittedMetricDescriptor = metricsEndpoint
                .metric("jvm.memory.committed", Arrays.asList("area:heap"));

        if (heapCommittedMetricDescriptor != null) {
            heapCommitted.set(heapCommittedMetricDescriptor
                    .getMeasurements()
                    .get(0)
                    .getValue());
        } else {
            heapCommitted.set(-1);
        }

        return heapCommitted;
    }

    public AtomicDouble getCpuUsage() {

        MetricsEndpoint.MetricDescriptor cpuUsageMetricDescriptor = metricsEndpoint
                .metric("system.cpu.usage", null);

        if (cpuUsageMetricDescriptor != null) {
            cpuUsed.set(cpuUsageMetricDescriptor
                    .getMeasurements()
                    .get(0)
                    .getValue());
        } else {
            cpuUsed.set(-1);
        }

        return cpuUsed;
    }

    public AtomicInteger getCpuCount() {

        MetricsEndpoint.MetricDescriptor cpuCountMetricDescriptor = metricsEndpoint
                .metric("system.cpu.count", null);

        if (cpuCountMetricDescriptor != null) {
            cpuCount.set(cpuCountMetricDescriptor
                    .getMeasurements()
                    .get(0)
                    .getValue().intValue());
        } else {
            cpuCount.set(-1);
        }

        return cpuCount;
    }
}
