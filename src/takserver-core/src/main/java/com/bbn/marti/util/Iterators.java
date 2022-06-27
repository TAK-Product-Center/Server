

package com.bbn.marti.util;

import java.util.Iterator;

import com.bbn.cot.filter.Filter;

public class Iterators {
	public static <E1,E2> Iterator<Tuple<E1,E2>> zip(Iterator<E1> left, Iterator<E2> right) {
		return new ZippedIterator<E1,E2>()
			.withLeft(left)
			.withRight(right);
	}

	public static <E> Iterator<E> filter(Iterator<E> iter, Filter<E> filter) {
		return new FilteringIterator<E>()
			.withIterator(iter)
			.withFilter(filter);
	}
}