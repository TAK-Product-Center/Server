

package com.bbn.marti.util;

import java.util.Iterator;

import com.bbn.cot.filter.Filter;

public class FilteringIterable<E> implements Iterable<E> {
	private Iterable<E> toFilter;
	private Filter<E> filter;
	
	public Iterator<E> iterator() {
		return Iterators.filter(toFilter.iterator(), filter);
	}

	public FilteringIterable<E> withIterable(Iterable<E> iterable) {
		this.toFilter = iterable;
		return this;
	}
	
	public FilteringIterable<E> withFilter(Filter<E> filter) {
		this.filter = filter;
		return this;
	}
}