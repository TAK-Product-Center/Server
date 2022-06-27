

package com.bbn.marti.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.bbn.cot.filter.Filter;

public class FilteringIterator<T> implements Iterator<T> {
	private Iterator<T> iter;
	private Filter<T> filter;

	public boolean hasNext() {
		return iter.hasNext();
	}
	
	public T next() {
		if (this.hasNext()) {
			T next = iter.next();
			return filter.filter(next);
		} else throw new NoSuchElementException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public FilteringIterator<T> withIterator(Iterator<T> iter) {
		this.iter = iter;
		return this;
	}
	
	public FilteringIterator<T> withFilter(Filter<T> filter) {
		this.filter = filter;
		return this;
	}
}