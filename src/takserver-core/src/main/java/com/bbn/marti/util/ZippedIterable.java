

package com.bbn.marti.util;

import java.util.Iterator;

public class ZippedIterable<E1,E2> implements Iterable<Tuple<E1,E2>> {
	private Iterable<E1> left;
	private Iterable<E2> right;
	
	public Iterator<Tuple<E1,E2>> iterator() {
		return Iterators.zip(
			left.iterator(),
			right.iterator());
	}
	
	public ZippedIterable<E1,E2> withLeft(Iterable<E1> left) {
		this.left = left;
		return this;
	}

	public ZippedIterable<E1,E2> withRight(Iterable<E2> right) {
		this.right = right;
		return this;
	}
}