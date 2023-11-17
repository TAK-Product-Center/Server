

package com.bbn.marti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.security.web.MartiValidatorConstants.Regex;

/**
 * Security constraints on an HTTP parameter.
 *
 */
public class HttpParameterConstraints {

	private static final Logger logger = LoggerFactory.getLogger(HttpParameterConstraints.class);

	public Regex validationPattern;
	public int maximumLength;

	/**
	 * Deprecated interface where the name of the regex is a magic string. Use the version that takes a regex instead.
	 * This method can return
	 * @param magicString
	 * @param maximumLength
	 */
	@Deprecated
	public HttpParameterConstraints(String magicString,
			int maximumLength)  {
		this.validationPattern = Regex.valueOf(magicString);
		if (this.validationPattern == null) {
			logger.warn("Invalid magic string \"" + magicString + "\". Validation regex is null!");
		}
		this.maximumLength = maximumLength;
	}

	public HttpParameterConstraints(Regex validationPattern, int maximumLength) {
		this.validationPattern = validationPattern;
		this.maximumLength = maximumLength;
	}
}


