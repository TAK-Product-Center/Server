

package com.bbn.marti.filter;

/**
* Google guava stand-in, for now
*/
public interface Predicate<T> {
	public boolean apply(T in);
}