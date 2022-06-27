

package com.bbn.marti.filter;

public class RejectingPredicate<T> implements Predicate<T> {
	private static final RejectingPredicate<Object> pred = new RejectingPredicate<Object>();
	
	public boolean apply(T in) {
		return false;
	}

	public static <E> RejectingPredicate<E> getInstance() {
		return (RejectingPredicate<E>) pred;
	}
}