

package com.bbn.marti.filter;

import java.util.Collection;

import tak.server.cot.CotEventContainer;


public class ContextNonemptyKeysPredicate implements Predicate<CotEventContainer> {
	private Iterable<String> keys;
	
	public boolean apply(CotEventContainer cot) {
		for (String key : keys) {
			Collection<?> checkNonempty = (Collection<?>) cot.getContext(key);
			if (checkNonempty == null ||
				checkNonempty.size() == 0) {
				return false;
			}
		}

		return true;
	}
	
	public ContextNonemptyKeysPredicate withContextKeys(Iterable<String> keys) {
		this.keys = keys;
		return this;
	}
}