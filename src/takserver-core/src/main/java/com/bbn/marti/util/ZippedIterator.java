

package com.bbn.marti.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ZippedIterator<E1,E2> implements Iterator<Tuple<E1,E2>> {
	private Iterator<E1> liter;
	private Iterator<E2> riter;
	
	public boolean hasNext() {
		return liter.hasNext() && riter.hasNext();
	}
	
	public Tuple<E1,E2> next() {
		if (hasNext()) {
			return new Tuple(liter.next(), riter.next());
		} else {
			throw new NoSuchElementException();
		} 
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public ZippedIterator<E1,E2> withLeft(Iterator<E1> left) {
		this.liter = left;
		return this;
	}
	
	public ZippedIterator<E1,E2> withRight(Iterator<E2> right) {
		this.riter = right;
		return this;
	}
}