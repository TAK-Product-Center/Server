

package com.bbn.marti.cot.search.service;

/**
 * Thin wrapper that aggregates the XML of a CoT event with necessary metadata.
 * At this time, the only metadata included is the time the Marti server received the event.
 *
 */
public class CotEventWrapper {
	protected final String content;
	protected final long receivedTime;
	protected final int primaryKey;
	
	public CotEventWrapper(String event, long time, int primaryKey) {
		this.content = event;
		this.receivedTime = time;
		this.primaryKey = primaryKey;
	}

	@Override
	public String toString() {
		return "CotEventWrapper [content=" + content + ", receivedTime=" + receivedTime + ", primaryKey=" + primaryKey
				+ "]";
	}
}