package com.bbn.marti.service;

import org.springframework.context.ApplicationEvent;

// Event triggered by removing a TAK Server subscription
public class RemoveSubscriptionEvent extends ApplicationEvent {

	private static final long serialVersionUID = 8429539362262946769L;

	public RemoveSubscriptionEvent(Object source) {
		super(source);
	}
}
