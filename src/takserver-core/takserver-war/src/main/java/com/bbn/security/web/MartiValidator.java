package com.bbn.security.web;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.ValidationErrorList;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.owasp.esapi.reference.DefaultValidator;
import org.owasp.esapi.reference.validation.DateValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Validator for Marti, which closely resembles the ESAPI
 * DefaultValidator. Differences from DefaultValidator:
 * <ul>
 * <li><code>assertValidHttpParameterSet</code> and related methods are
 * case-insensitive</li>
 * </ul>
 *
 *
 */
public class MartiValidator extends DefaultValidator {

//	public static final int LONG_STRING_CHARS = 2047;
//	public static final int DEFAULT_STRING_CHARS = 255;
//	public static final int SHORT_STRING_CHARS = 128;

	private static final Logger logger = LoggerFactory.getLogger(com.bbn.security.web.MartiValidator.class);

	/**
	 * Members of this enum map to regexes defined in
	 * apache-tomcat/lib/validation.properties
	 * 
	 *
	 */
//	public enum Regex {
//	
//	    CertCommonName,     		// allow only word characters, whitespace, ',' and '='
//		Coordinates, 				// decimal latitude or longitude
//		ConfigAttribute, 			// attribute name for Core Config
//		CotType,					// CoT type such as "a-f-.-u"
//		Double,						// signed or unsigned decimal
//		DirectoryName,     		 	// POSIX directory name
//		Hexidecimal,				// hexidecimal numbers
//		KmlGeometry,				// <Point><coordinates>  tag in KML
//		MartiSafeString, 			// Alphanumeric plus certain special characters such _, -, :, /
//		NonNegativeInteger, 		// Digits only
//		URL,						// http, https, ftp, and ftps URLs
//		SafeString,					// Alphanumeric plus space character
//		RestrictedRegex,			// Simple regex patterns (no grouping) for pattern matches
//		SupportedProtocol,  		// STCP, TCP, or UDP (case insensitive)
//		Timestamp,					// CoT timestamp,
//		WordList,					// comma-separated list of alphanumeric words
//		XmlBlackList,				// disallowed strings for XML
//		XmlBlackListWordOnly, 		// disallowed strings for XML, relaxed to allow 'script' as substring, ex <description>
//		XpathBlackList,				// disallowed string for XPath expressions
//		VideoURL,					// similar to URL, but includes addition protocols for video streaming
//		Filename,					// valid filenames
//		PreventDirectoryTraversal	// disallow attempts at directory traversal by not allowing .. in paths
//	}

	public MartiValidator() {
		super();
	}

	public MartiValidator(Encoder encoder) {
		super(encoder);
	}

	@Override
	public Date getValidDate(String context, String input, DateFormat format, boolean allowNull)
			throws ValidationException, IntrusionException {
		DateValidationRule rule = new DateValidationRule("CotDate", ESAPI.encoder(), format);
		rule.setAllowNull(allowNull);
		String toValidate = (input == null) ? null : input.trim();
		return rule.getValid(context, toValidate);
	}

	@Override
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull)
			throws ValidationException {
		String toValidate = (input == null) ? null : input.trim();
		if (logger.isDebugEnabled()) {
			logger.debug("getValidInput context={} input={} type={} maxLength={} allowNull={}", context, input, type,
					maxLength, allowNull);
		}
		return super.getValidInput(context, toValidate, type, maxLength, allowNull);
	}

	@Override
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			boolean canonicalize) throws ValidationException {
		String toValidate = (input == null) ? null : input.trim();
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, canonicalize);
	}

	@Override
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			ValidationErrorList errors) {
		String toValidate = (input == null) ? null : input.trim();
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, errors);
	}

	@Override
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			boolean canonicalize, ValidationErrorList errors) {
		String toValidate = (input == null) ? null : input.trim();
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, canonicalize, errors);
	}
}