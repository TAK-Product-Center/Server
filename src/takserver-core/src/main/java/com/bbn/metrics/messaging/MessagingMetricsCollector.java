package com.bbn.metrics.messaging;

import com.bbn.metrics.service.ActuatorMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessagingMetricsCollector {

    private final ActuatorMetricsService actuatorMetricsService;

    @Autowired
    public MessagingMetricsCollector(ActuatorMetricsService actuatorMetricsService) {
        this.actuatorMetricsService = actuatorMetricsService;
    }

    public double getCpuUsage() {
        return actuatorMetricsService.getCpuUsage().get();
    }

    public int getCpuCount() {
        return actuatorMetricsService.getCpuCount().get();
    }

    public double getHeapCommitted() {
        return actuatorMetricsService.getHeapCommitted().get();
    }

    public double getHeapUsed() {
        return actuatorMetricsService.getHeapUsed().get();
    }
}
