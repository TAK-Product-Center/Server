

package com.bbn.cot.filter;

import java.util.HashMap;

import org.apache.log4j.Logger;

import tak.server.cot.CotEventContainer;

public class DropEventFilter implements CotFilter {
	private String type;
	private String detail;
	long threshold = -1;
	HashMap<String, Long> seenList = new HashMap<String, Long>();

	private static final Logger log = Logger.getLogger(DropEventFilter.class);

	public DropEventFilter(String type, String detail, long threshold) {
		this.type = type;
		this.detail = detail;
		this.threshold = threshold;
		if (this.threshold != -1) {
			this.threshold *= 1000;
		}
	}

	@Override
	public CotEventContainer filter(CotEventContainer c) {
		if(type != null && type.compareTo(c.getType()) != 0) {
			if (log.isDebugEnabled()) {
				log.debug("cot event type : " + c.getType() + " didnt match filter type : " + type);
			}
			return c;
		}

		if(detail != null && !c.getDetailXml().contains(detail)) {
			if (log.isDebugEnabled()) {
				log.debug("cot event type : " + c.getDetailXml() + " didnt match filter detail: " + detail);
			}
			return c;
		}

		// if we had a match on type and dont have a threshold set, block the message
		if (threshold == -1) {
			return null;
		}

		if(seenList.containsKey(c.getUid())) {
			long ctime = System.currentTimeMillis();
			if((seenList.get(c.getUid()) + threshold) < ctime) {
				if (log.isDebugEnabled()) {
					log.debug("threshold exceeded for cot type : " + c.getType() + ", uid : " + c.getUid());
				}
				seenList.put(c.getUid(), ctime);
				return c;
			} else {
				if (log.isDebugEnabled()) {
					log.debug("within threshold for cot type : " + c.getType() + ", uid : " + c.getUid());
				}
				return null;
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("first time seeing cot type : " + c.getType() + ", uid : " + c.getUid());
			}
			seenList.put(c.getUid(), System.currentTimeMillis());
			return c;
		}
	}
}
