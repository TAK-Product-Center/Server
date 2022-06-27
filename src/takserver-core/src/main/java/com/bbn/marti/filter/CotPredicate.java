

package com.bbn.marti.filter;

import tak.server.cot.CotEventContainer;

public interface CotPredicate extends Predicate<CotEventContainer> {
	public boolean apply(CotEventContainer in);
}