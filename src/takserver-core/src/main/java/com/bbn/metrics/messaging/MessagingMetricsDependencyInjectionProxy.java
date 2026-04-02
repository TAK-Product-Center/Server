
package com.bbn.metrics.messaging;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MessagingMetricsDependencyInjectionProxy implements ApplicationContextAware {

    private static ApplicationContext springContext;

    private volatile static MessagingMetricsDependencyInjectionProxy instance = null;

    public static MessagingMetricsDependencyInjectionProxy getInstance() {
        if (instance == null) {
            synchronized (MessagingMetricsDependencyInjectionProxy.class) {
                if (instance == null) {
                    instance = springContext.getBean(MessagingMetricsDependencyInjectionProxy.class);
                }
            }
        }
        return instance;
    }

    public static ApplicationContext getSpringContext() {
        return springContext;
    }

    @SuppressWarnings("static-access")
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.springContext = context;
    }

    private volatile MessagingMetricsService messagingMetricsService = null;

    public MessagingMetricsService messagingMetricsService() {
        if (messagingMetricsService == null) {
            synchronized (this) {
                if (messagingMetricsService == null) {
                    messagingMetricsService = springContext.getBean(MessagingMetricsService.class);
                }
            }
        }
        return messagingMetricsService;
    }

    private volatile MessagingMetricsCollector messagingMetricsCollector = null;

    public MessagingMetricsCollector messagingMetricsCollector() {
        if (messagingMetricsCollector == null) {
            synchronized (this) {
                if (messagingMetricsCollector == null) {
                    messagingMetricsCollector = springContext.getBean(MessagingMetricsCollector.class);
                }
            }
        }
        return messagingMetricsCollector;
    }
}
