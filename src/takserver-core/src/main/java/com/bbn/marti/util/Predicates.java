

package com.bbn.marti.util;

import java.util.Arrays;

import org.dom4j.DocumentHelper;
import org.dom4j.InvalidXPathException;
import org.dom4j.XPath;

import com.bbn.marti.filter.AcceptingPredicate;
import com.bbn.marti.filter.ContextContainsKeysPredicate;
import com.bbn.marti.filter.ContextNonemptyKeysPredicate;
import com.bbn.marti.filter.Predicate;
import com.bbn.marti.filter.XPathPredicate;

import tak.server.cot.CotEventContainer;

/**
* Static utilities class for creating predicates
*/
public class Predicates {
	/**
	* Encapsulates logic for xpath meaning -- will return the always accepting
	* xpath filter for empty/null/trimmed empty
	*/
	public static Predicate<CotEventContainer> xpathPredicate(String xpath)
		throws InvalidXPathException 
	{
		Predicate<CotEventContainer> result;
		if (xpath == null ||
			xpath.trim().length() == 0 ||
			xpath.trim().equals("*")) {

			result = AcceptingPredicate.getInstance();
		} else {
			XPath xpathPredicate = DocumentHelper.createXPath(xpath);
			result = new XPathPredicate()
				.withXPath(xpathPredicate);
		}

		return result;
	}

	public static Predicate<CotEventContainer> containsContextKeysPredicate(String... contextKeys) {
		return containsContextKeysPredicate(Arrays.asList(contextKeys));
	}

	public static Predicate<CotEventContainer> containsContextKeysPredicate(Iterable<String> contextKeys) {
		return new ContextContainsKeysPredicate()
			.withContextKeys(contextKeys);
	}

	public static Predicate<CotEventContainer> nonemptyContextKeysPredicate(String... contextKeys) {
		return nonemptyContextKeysPredicate(Arrays.asList(contextKeys));
	}

	public static Predicate<CotEventContainer> nonemptyContextKeysPredicate(Iterable<String> contextKeys) {
		return new ContextNonemptyKeysPredicate()
			.withContextKeys(contextKeys);
	}
}