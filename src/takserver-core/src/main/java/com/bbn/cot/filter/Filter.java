

package com.bbn.cot.filter;

public interface Filter<T> {
	public T filter(T in);
}