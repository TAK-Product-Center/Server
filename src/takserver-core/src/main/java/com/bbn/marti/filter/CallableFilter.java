

package com.bbn.marti.filter;

import java.util.concurrent.Callable;

import com.bbn.cot.filter.Filter;

public class CallableFilter<T> implements Callable<T> {
	private Filter<T> filter;
	private T input;
	
	public T call() {
		return filter.filter(input);
	}

	public CallableFilter<T> withFilter(Filter<T> filter) {
		this.filter = filter;
		return this;
	}
	
	public CallableFilter<T> withInput(T in) {
		this.input = in;
		return this;
	}
}

