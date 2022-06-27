

package com.bbn.marti.util;

import com.bbn.marti.service.Service;

public class Services {

	
	
	public static <E> boolean hook(Service<E> producer, Service<E> consumer) {
		if (producer != consumer) {
			return producer.consumers().add(consumer);
		}
		
		return false;
	}

	public static <E> boolean unhook(Service<E> producer, Service<E> consumer) {
		if (producer != consumer) {
			return producer.consumers().remove(consumer);
		}
		
		return false;
	}
	
	public static <E> void startAll(Iterable<Service<E>> services) { 
		for (Service<E> service : services) {
			service.start();
		}
	}

	public static <E> void stopAll(Iterable<Service<E>> services) {
		for (Service<E> service : services) {
			service.stop();
		}
	}
}