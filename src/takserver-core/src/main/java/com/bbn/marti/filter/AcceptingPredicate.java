

package com.bbn.marti.filter;

public class AcceptingPredicate<T> implements Predicate<T> {
	private final static AcceptingPredicate<Object> pred = new AcceptingPredicate<Object>();
	
	public boolean apply(T in) {
		return true;
	}
	
	public static <T> AcceptingPredicate<T> getInstance() {
		return (AcceptingPredicate<T>) pred;
	}
}