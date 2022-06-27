

package com.bbn.marti.service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import tak.server.cot.CotEventContainer;

public abstract class BaseService {
	private static final Logger log = Logger.getLogger(BaseService.class
			.getSimpleName());

	protected AtomicBoolean keepGoing = new AtomicBoolean(false);
	protected Thread runThread = null;
	protected List<BaseService> consumers = new LinkedList<BaseService>();

	public boolean isKeepGoing() {
	    return keepGoing.get();
	}
	
	public void startService() {
	 
		log.debug("Starting " + name());

		keepGoing.set(true);
		runThread = new Thread(new Runnable() {
			public void run() {
				while (keepGoing.get()) {
					try {
					  processNextEvent();
					} catch (Throwable thrown) {
					  log.error("Error processing event in service " + name(), thrown);
					}
				}
			}
		});
		runThread.setName("Service:"+this.name());
		runThread.start();
	}

	public void stopService(boolean wait) {
		if (runThread == null)
			return;
		keepGoing.set(false);
		runThread.interrupt();
		if (wait) {
			try {
				runThread.join();
			} catch (InterruptedException e) {
				log.warn("Interrupted while waiting for " + this.name()
						+ " service to stop.");
			}
		}
	}

	// TODO: add a filter argument so that consumers can be selective
	public void addConsumer(BaseService b) {
		consumers.add(b);
	}
	
	abstract public boolean hasRoomInQueueFor(CotEventContainer c);

	abstract public String name();

	abstract public boolean addToInputQueue(CotEventContainer c);

	abstract protected void processNextEvent();
}
