package com.bbn.marti.service;

import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 */
public class DistributedFederationHttpConnectorManager implements FederationHttpConnectorManager, org.apache.ignite.services.Service {
	private static final long serialVersionUID = 6786364255099592302L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedFederationHttpConnectorManager.class);
	private static final int FED_HTTPS_CONNECTOR_PORT = 8444;

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public void asyncReloadFederationHttpConnector() {
		try {
			Executors.newSingleThreadExecutor().submit(new Runnable() {

				@Override
				public void run() {
					String objectString = "*:type=Connector,port=" + FED_HTTPS_CONNECTOR_PORT + ",*";

					try {
						final ObjectName objectNameQuery = new ObjectName(objectString);

						for (final MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
							if (server.queryNames(objectNameQuery, null).size() > 0) {
								MBeanServer mbeanServer = server;
								ObjectName objectName = (ObjectName) server.queryNames(objectNameQuery, null).toArray()[0];

								mbeanServer.invoke(objectName, "stop", null, null);
								long start = System.currentTimeMillis();
								long max_duration = 6000L;
								long duration = 0L;
								do {
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
									}

									duration = (System.currentTimeMillis() - start);

								} while (duration < max_duration && server.queryNames(objectNameQuery, null).size() > 0);

								mbeanServer.invoke(objectName, "start", null, null);

								break;
							}
						}
					} catch (Exception e) {
						logger.warn("exception reloading fed truststore", e);
					}
				}});
		} catch (Exception e) {
			logger.info("error reloading federation http connector " + e);
		}
	}
	
}
