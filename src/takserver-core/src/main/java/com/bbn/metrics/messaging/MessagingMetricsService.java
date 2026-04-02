
package com.bbn.metrics.messaging;

public interface MessagingMetricsService {
    double getCpuUsage();

    int getCpuCount();

    double getHeapCommitted();

    double getHeapUsed();
}