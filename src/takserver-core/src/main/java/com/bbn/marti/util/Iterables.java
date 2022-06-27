

package com.bbn.marti.util;

import com.bbn.cot.filter.Filter;

public class Iterables {
	public static <E1,E2> Iterable<Tuple<E1,E2>> zip(Iterable<E1> left, Iterable<E2> right) {
		return new ZippedIterable<E1,E2>()
			.withLeft(left)
			.withRight(right);
	}

	public static <E> Iterable<E> filter(Iterable<E> toFilter, Filter<E> filter) {
		return new FilteringIterable<E>()
			.withIterable(toFilter)
			.withFilter(filter);
	}
}