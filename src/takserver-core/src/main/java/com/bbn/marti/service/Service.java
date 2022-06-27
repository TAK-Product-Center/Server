

package com.bbn.marti.service;

import com.bbn.marti.util.Consumer;
import com.bbn.marti.util.ListenerOutlet;

public interface Service<E> extends Consumer<E> {
	public enum State {
		NEW,
		STARTING,
		RUNNING,
		STOPPING,
		TERMINATED
	}

	public void start();
	public void stop();

	public State state();
	public String name();

	public ListenerOutlet<E> consumers();
	
	public boolean wants(E in);
	public boolean submit(E in);
}