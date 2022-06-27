

package com.bbn.marti.filter;

import com.bbn.cot.filter.Filter;

public class ComposedFilter<T> implements Filter<T> {
	Iterable<Filter<T>> filters;
	
	public T filter(T in) {
		for (Filter<T> filter : filters) {
			in = filter.filter(in);
		}
		
		return in;
	}

	public ComposedFilter<T> withFilters(Iterable<Filter<T>> filters) {
		this.filters = filters;
		return this;
	}
}
