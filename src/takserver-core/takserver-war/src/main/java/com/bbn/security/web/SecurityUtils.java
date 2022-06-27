

package com.bbn.security.web;

import java.util.Map;
import java.util.logging.Logger;

import org.owasp.esapi.errors.ValidationException;

/**
 * Miscellaneous utilities to help with ESAPI usage.
 * All methods in this class should be static.
 *
 */
public class SecurityUtils {
	
	public static Logger log = Logger.getLogger(SecurityUtils.class.getCanonicalName());
		
	/**
	 * Retrieves the value from an HTTPServletRequest's parameter map using a case-insensitive string match.
	 * 
	 * @param map any map of the same type as returned by HttpServletRequest.getParameterMap()
	 * @param caseInsensitiveName key to match in a case-insensitive way
	 * @return the value of the matching key, or <code>null</code> if the key is not present.
	 */
	public static String[] getCaseInsensitiveParameter(Map<String, String[]> map, String caseInsensitiveName) {
		String[] result = null;
		for (String key : map.keySet()) {
			if (key.compareToIgnoreCase(caseInsensitiveName) == 0) {
				result = map.get(key);
				break;
			}
		}
		return result;
	}

	/**
	 * Parses a Double from a String, safely handling <code>null</code> or empty input.
	 * @param text string to parse.
	 * @param defaultValue default value to return if input is <code>null</code> or empty
	 * @return the numeric value of the string
	 * @throws ValidationException if the input contains non-numeric characters
	 */
	public static Double parseDouble(String text, Double defaultValue) 
			throws ValidationException {
		Double value = defaultValue;
		if (!text.isEmpty()) {
			try {
				value = Double.parseDouble(text);
			} catch (NullPointerException npe) {
				// No problem, just keep the default value
			} catch (NumberFormatException ex) {
				throw new ValidationException("Illegal decimal format", "Illegal decimal format", ex);
			} 
		}
		return value;
	}
	
	/**
	 * Parses a Integer from a String, safely handling <code>null</code> or empty input.
	 * @param text string to parse.
	 * @param defaultValue default value to return if input is <code>null</code> or empty
	 * @return the numeric value of the string
	 * @throws ValidationException if the input contains non-numeric characters
	 */
	public static Integer parseInteger(String text, Integer defaultValue) 
			throws ValidationException {
		Integer value = defaultValue;
		if (!text.isEmpty()) {
			try {
				value = Integer.parseInt(text);
			} catch (NullPointerException npe) {
				// No problem, just keep the default value
			} catch (NumberFormatException ex) {
				throw new ValidationException("Illegal integer format", "Illegal integer format", ex);
			} 
		}
		return value;
	}
	
	/**
	 * Parses a Long from a String, safely handling <code>null</code> or empty input.
	 * @param text string to parse.
	 * @param defaultValue default value to return if input is <code>null</code> or empty
	 * @return the numeric value of the string
	 * @throws ValidationException if the input contains non-numeric characters
	 */
	public static Long parseLong(String text, Long defaultValue) 
			throws ValidationException {
		Long value = defaultValue;
		if (!text.isEmpty()) {
			try {
				value = Long.parseLong(text);
			} catch (NullPointerException npe) {
				// No problem, just keep the default value
			} catch (NumberFormatException ex) {
				throw new ValidationException("Illegal long-integer format", "Illegal long-integer format", ex);
			} 
		}
		return value;
	}
	
	/**
	 * Like the three-argument version of parseDoubleWithoutExceptions, but with no logging of <code>null</code> or 
	 * ill-formatted 
	 * input.
	 */
	public static Double parseDoubleWithoutExceptions(String text, Double defaultValue) {
		return SecurityUtils.parseDoubleWithoutExceptions(text, defaultValue, null);
	}
	
	/**
	 * Safely parses a Double from a String and returns the default value if the string is null or ill-formatted
	 * @param text String to parse; should contain only numeric characters
	 * @param defaultValue Default value to return if the string does not parse. May be <code>null</code> if that's 
	 * what you want. 
	 * @param logger Optional logger to write a warning if a parse error occurs. If <code>null</code>, no log messages
	 * will be written.
	 * @return The numeric value of the string, or the given default if the string did not parse.
	 */
	public static Double parseDoubleWithoutExceptions(String text, Double defaultValue, 
			java.util.logging.Logger logger) {
		Double value = defaultValue;
		try {
			value = Double.parseDouble(text);
		} catch (NumberFormatException ex) {
			if (logger != null) {
				logger.warning("Invalid decimal format detected, using default value of " + defaultValue);
			}
		}
		return value;
	}
	
	/**
	 * Like the three-argument version of parseIntegerWithoutExceptions, but with no logging of <code>null</code> or 
	 * ill-formatted 
	 * input.
	 */
	public static Integer parseIntegerWithoutExceptions(String text, Integer defaultValue) {
		return SecurityUtils.parseIntegerWithoutExceptions(text, defaultValue, null);
	}
	
	/**
	 * Safely parses an Integer from a String and returns the default value if the string is null or ill-formatted
	 * @param text String to parse; should contain only numeric characters
	 * @param defaultValue Default value to return if the string does not parse. May be <code>null</code> if that's 
	 * what you want. 
	 * @param logger Optional logger to write a warning if a parse error occurs. If <code>null</code>, no log messages
	 * will be written.
	 * @return The numeric value of the string, or the given default if the string did not parse.
	 */
	public static Integer parseIntegerWithoutExceptions(String text, Integer defaultValue, 
			java.util.logging.Logger logger) {
		Integer value = defaultValue;
		try {
			value = Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			if (logger != null) {
				logger.warning("Invalid integer format detected, using default value of " + defaultValue);
			}
		} catch (NullPointerException ex) {
			// No problem. Just keep the default value
		}
		return value;
	}
	
	
	/**
	 * Like the three-argument version of parseLongWithoutExceptions, but with no logging of <code>null</code> or 
	 * ill-formatted 
	 * input.
	 */
	public static Long parseLongWithoutExceptions(String text, Long defaultValue) {
		return SecurityUtils.parseLongWithoutExceptions(text, defaultValue, null);
	}
	
	/**
	 * Safely parses a Long from a String and returns the default value if the string is null or ill-formatted
	 * @param text String to parse; should contain only numeric characters
	 * @param defaultValue Default value to return if the string does not parse. May be <code>null</code> if that's 
	 * what you want. 
	 * @param logger Optional logger to write a warning if a parse error occurs. If <code>null</code>, no log messages
	 * will be written.
	 * @return The numeric value of the string, or the given default if the string did not parse.
	 */
	public static Long parseLongWithoutExceptions(String text, Long defaultValue, java.util.logging.Logger logger) {
		Long value = defaultValue;
		try {
			value = Long.parseLong(text);
		} catch (NumberFormatException ex) {
			if (logger != null) {
				logger.warning("Invalid decimal format detected, using default value of " + defaultValue);
			}
		}
		return value;
	}
}
