

package com.bbn.cot.filter;
	
import tak.server.cot.CotEventContainer;

public interface CotFilter extends Filter<CotEventContainer> {
	public CotEventContainer filter(CotEventContainer c);
}
