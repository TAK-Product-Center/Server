

package com.bbn.marti.filter;

import org.dom4j.XPath;

import tak.server.cot.CotEventContainer;

public class XPathPredicate implements Predicate<CotEventContainer> {
	private XPath xpath;
	
	public boolean apply(CotEventContainer cot) {
		return xpath.matches(cot.getDocument());
	}
	
	public XPathPredicate withXPath(XPath xpath) {
		this.xpath = xpath;
		return this;
	}
}