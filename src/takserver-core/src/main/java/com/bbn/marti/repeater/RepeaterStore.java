package com.bbn.marti.repeater;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tak.server.cot.CotEventContainer;

/*
 * In-memory data structures for managing repeaters. Can be refactored into an interface and implementation when needed.
 * 
 */
public class RepeaterStore {
	
	private Map<String, RepeatableContainer> repeatedMessages = new ConcurrentHashMap<>();	
	private Map<String, CotEventContainer> cancelledMessages = new ConcurrentHashMap<>();
	
	public Map<String, RepeatableContainer> getRepeatedMessages() {
		return repeatedMessages;
	}
	
	public void setRepeatedMessages(Map<String, RepeatableContainer> repeatedMessages) {
		this.repeatedMessages = repeatedMessages;
	}
	
	public Map<String, CotEventContainer> getCancelledMessages() {
		return cancelledMessages;
	}
	public void setCancelledMessages(Map<String, CotEventContainer> cancelledMessages) {
		this.cancelledMessages = cancelledMessages;
	}
}
