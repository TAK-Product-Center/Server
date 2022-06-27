

package com.bbn.marti.filter;

import com.bbn.cot.filter.Filter;

public class PredicateFilter<T> implements Filter<T> {
	Predicate<T> predicate;
	Filter<T> filter;
	
	public T filter(T in) {
		if (predicate.apply(in)) {
			in = filter.filter(in);
		}
		
		return in;
	}

	public PredicateFilter<T> withPredicate(Predicate<T> predicate) {
		this.predicate = predicate;
		return this;
	}
	
	public PredicateFilter<T> withFilter(Filter<T> filter) {
		this.filter = filter;
		return this;
	}
}