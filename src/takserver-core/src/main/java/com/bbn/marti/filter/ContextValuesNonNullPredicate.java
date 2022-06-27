

package com.bbn.marti.filter;

import tak.server.cot.CotEventContainer;

public class ContextValuesNonNullPredicate implements Predicate<CotEventContainer> {
	private Iterable<String> keysToCheck;

	/**
	* Returns true iff all of the given keys are present in the cot container's 
	* context map
	*/
	public boolean apply(CotEventContainer cot) {
		for (String key : keysToCheck) {
			if (!cot.hasContextKey(key) || cot.getContext(key) == null) {
				return false;
			}
		}
		
		return true;
	}
	
	public ContextValuesNonNullPredicate withContextKeys(Iterable<String> keys) {
		this.keysToCheck = keys;
		return this;
	}
}