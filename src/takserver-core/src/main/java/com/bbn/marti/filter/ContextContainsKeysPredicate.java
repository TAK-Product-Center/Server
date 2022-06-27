

package com.bbn.marti.filter;

import tak.server.cot.CotEventContainer;

public class ContextContainsKeysPredicate implements Predicate<CotEventContainer> {
	private Iterable<String> keysToCheck;

	/**
	* Returns true iff all of the given keys are present in the cot container's 
	* context map
	*/
	public boolean apply(CotEventContainer cot) {
		for (String key : keysToCheck) {
			if (cot.hasContextKey(key)) {
				return false;
			}
		}
		
		return true;
	}
	
	public ContextContainsKeysPredicate withContextKeys(Iterable<String> keys) {
		this.keysToCheck = keys;
		return this;
	}
}