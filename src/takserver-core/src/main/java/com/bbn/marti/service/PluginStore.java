package com.bbn.marti.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.util.spring.SpringContextBeanForApi;

public class PluginStore {
	private static final Logger logger = LoggerFactory.getLogger(SubscriptionStore.class);

	private final AtomicInteger interceptorsActive = new AtomicInteger(0);

	private static PluginStore instance;

	public static synchronized PluginStore getInstance() {
		if (instance == null) {
			synchronized (SubscriptionStore.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(PluginStore.class);
				}
			}
		}

		return instance;
	}
	
	public void disableInterception() {
		interceptorsActive.getAndSet(0);
	}

	public void addInterceptorPluginsActive(int n) {
		interceptorsActive.getAndAdd(n);
	}

	public int getInterceptorPluginsActive() {
		return interceptorsActive.get();
	}
}
