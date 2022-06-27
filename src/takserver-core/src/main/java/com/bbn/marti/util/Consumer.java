

package com.bbn.marti.util;

public interface Consumer<E> {
	public boolean wants(E input);
	public boolean submit(E input);
}