

package com.bbn.cot.filter;

import org.apache.log4j.Logger;

import tak.server.cot.CotEventContainer;

public abstract class PriorityFilter implements CotFilter {
	protected Logger log;
	private String priorityPrefix = "";
	public static final int DEFAULT_PRIORITY = -1; // anything < 0

	public PriorityFilter(String prefix) {
		log = Logger.getLogger(this.getClass());
		priorityPrefix = prefix;
	}

	@Override
	public CotEventContainer filter(CotEventContainer c) {
		log.debug(this.getClass().getName() + " filtering object");
		try {
			double priority = getPriority(c);
			log.debug(this.getClass().getName() + " assigned priority "
					+ priority);
			c.setContextValue(priorityPrefix + "priority", priority);
		} catch (Exception e) {
			log.debug("Could not assign a priority to " + c + " because " + e);
		}
		return c;
	}

	protected abstract int getPriority(CotEventContainer c);
}
