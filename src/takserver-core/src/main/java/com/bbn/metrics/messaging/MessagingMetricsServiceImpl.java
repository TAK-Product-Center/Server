
package com.bbn.metrics.messaging;

import com.bbn.metrics.service.ActuatorMetricsService;
import org.apache.ignite.services.Service;

public class MessagingMetricsServiceImpl implements MessagingMetricsService, Service {
    private static final long serialVersionUID = 6841290456723893412L;

    @Override
    public double getCpuUsage() {
        return MessagingMetricsDependencyInjectionProxy.getInstance().messagingMetricsCollector().getCpuUsage();
    }

    @Override
    public int getCpuCount() {
        return MessagingMetricsDependencyInjectionProxy.getInstance().messagingMetricsCollector().getCpuCount();
    }

    @Override
    public double getHeapCommitted() {
        return MessagingMetricsDependencyInjectionProxy.getInstance().messagingMetricsCollector().getHeapCommitted();
    }

    @Override
    public double getHeapUsed() {
        return MessagingMetricsDependencyInjectionProxy.getInstance().messagingMetricsCollector().getHeapUsed();
    }

    @Override
    public void cancel() { }

    @Override
    public void init() { }

    @Override
    public void execute() { }
}