package com.bbn.marti.service;

import org.springframework.context.ApplicationEvent;

// Event triggered by adding a TAK Server subscription
public class AddSubscriptionEvent extends ApplicationEvent {

	private static final long serialVersionUID = 4459757943342776955L;

	public AddSubscriptionEvent(Object source) {
		super(source);
	}
}
