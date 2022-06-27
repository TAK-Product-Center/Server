

package com.bbn.marti.util;

import java.util.concurrent.Callable;

import com.bbn.cot.filter.Filter;
import com.bbn.marti.filter.CallableFilter;
import com.bbn.marti.filter.ComposedFilter;
import com.bbn.marti.filter.Predicate;
import com.bbn.marti.filter.PredicateFilter;

/**
* Static utilities for manipulating and composing filters
*
*/
public class Filters {
	public static <T> Filter<T> compose(Iterable<Filter<T>> filters) {
		return new ComposedFilter<T>()
			.withFilters(filters);
	}

	/**
	* Returns a filter wrapping the given one that will be applied only if the 
	* predicate returns true on the input. Otherwise, the input is returned.
	*/
	public static <T> Filter<T> predicate(Filter<T> filter, Predicate<T> predicate) {
		return new PredicateFilter<T>()
			.withFilter(filter)
			.withPredicate(predicate);
	}

	/**
	* 
	*/
	public static <T> Callable<T> wrapWithCallable(Filter<T> filter, T in) {
		return new CallableFilter<T>()
			.withFilter(filter)
			.withInput(in);
	}
}