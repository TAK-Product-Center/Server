package com.bbn.security.web;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
/*
 The BSD License

Copyright (c) 2007, The OWASP Foundation
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
Neither the name of the OWASP Foundation nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

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
 * Custom Validator for Marti, which closely resembles the ESAPI DefaultValidator.
 * Differences from DefaultValidator:
 * <ul>
 * <li><code>assertValidHttpParameterSet</code> and related methods are case-insensitive</li>
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
	 * Members of this enum map to regexes defined in apache-tomcat/lib/validation.properties 
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
	 
	/**
	 * Uses case-insensitive comparison to validate that the parameters in the current request contain all required 
	 * parameters and only optional ones in addition. Invalid input will generate a descriptive ValidationException, 
	 * and input that is clearly an attack will generate a descriptive IntrusionException.
	 *
	 */
	@Override
	public void assertValidHTTPRequestParameterSet(String context, 
													HttpServletRequest request, 
													Set<String> required, 
													Set<String> optional) 
		throws ValidationException, IntrusionException {
		
		if (logger.isDebugEnabled()) {
			logger.debug(" assertValidHTTPRequestParameterSet. context: " + context + " request: " + request
					+ " required: " + required
					+ " optional: " + optional);
		}
		
		// This implementation in inefficient but performance is not much a of a concern.
		
		Set<String> given = request.getParameterMap().keySet();
		Set<String> lowerCase = new HashSet<String>();
		for (String key : given) {
			lowerCase.add(key.toLowerCase());
		}

		// verify ALL required parameters are present		
		Set<String> missing = new HashSet<String>();
		for (String parameter : required) {
			if (!lowerCase.contains(parameter.toLowerCase())) {
				missing.add(parameter);
			}
		}
		if (missing.size() > 0) {

			String message = "Invalid HTTP request missing parameters " + missing + ": context=" + context;
			
			throw new ValidationException(message, message, context);
		}

		// verify ONLY optional + required parameters are present
		Set<String> allowed = new HashSet<String>();
		for (String parameter : required) {
			allowed.add(parameter.toLowerCase());
		}
		for (String parameter : optional) {
			allowed.add(parameter.toLowerCase());
		}
		
		Set<String> extra = new HashSet<String>();
		for (String parameter : given) {
			if (!allowed.contains(parameter.toLowerCase())) {
				extra.add(parameter);
			}
		}
		
		if (extra.size() > 0) {
			Iterator<String> extraItr = extra.iterator();
			StringBuilder badParameters = new StringBuilder();
			while (extraItr.hasNext()) {
				String badExtra = extraItr.next();
				if (this.isValidInput("assertValidHttpRequestParameterSet", badExtra, "HTTPParameterName",
						MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
					badParameters.append(badExtra);
				} else {
					badParameters.append("[redacted]");
				}
				if (extraItr.hasNext()) {
					badParameters.append(", ");
				}
			}
			
			throw new ValidationException( context + ": Invalid HTTP request extra parameters (redacted) ", 
					"Invalid HTTP request extra parameters " + badParameters.toString() + ": context=" + context, context );
		}
	}
	
	@Override
	public Date getValidDate(String context, String input, DateFormat format, boolean allowNull) 
			throws ValidationException, IntrusionException {
		DateValidationRule dvr = new DateValidationRule( "CotDate", ESAPI.encoder(), format);
		dvr.setAllowNull(allowNull);
		String toValidate = (input == null) ? input : input.trim();
		return dvr.getValid(context, toValidate); 
	}
	
	/**
	 * Like <code>DefaultValidator.getValidInput</code> but ignoring automatically trims white space from the input.
	 */
	@Override 
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull) throws ValidationException {
		String toValidate = (input == null) ? input : input.trim();
		
		if (logger.isDebugEnabled()) {
			logger.debug("getValidInput context " + context + " input: " + input + " type: " + type + " maxLength: " + maxLength + " allowNull: " + allowNull);
		}
		
		return super.getValidInput(context, toValidate, type, maxLength, allowNull);
	}
	
	/**
	 * Like <code>DefaultValidator.getValidInput</code> but ignoring automatically trims white space from the input.
	 */
	@Override 
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			boolean canonicalize) throws ValidationException {
		String toValidate = (input == null) ? input : input.trim();
		
		if (logger.isDebugEnabled()) {
			logger.debug("getValidInput context " + context + " input: " + input + " type: " + type + " maxLength: " + maxLength + " allowNull: " + allowNull + " canonicalize: " + canonicalize);
		}
		
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, canonicalize);
	}
	
	/**
	 * Like <code>DefaultValidator.getValidInput</code> but ignoring automatically trims white space from the input.
	 */
	@Override 
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			 ValidationErrorList errorList) {
		String toValidate = (input == null) ? input : input.trim();
		
		
		if (logger.isDebugEnabled()) {
			logger.debug("getValidInput context " + context + " input: " + input + " type: " + type + " maxLength: " + maxLength + " allowNull: " + allowNull + " validationErrorList: " + errorList);
		}
		
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, errorList);
	}
	
	/**
	 * Like <code>DefaultValidator.getValidInput</code> but ignoring automatically trims white space from the input.
	 */
	@Override 
	public String getValidInput(String context, String input, String type, int maxLength, boolean allowNull,
			boolean canonicalize, ValidationErrorList errorList) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("getValidInput context " + context + " input: " + input + " type: " + type + " maxLength: " + maxLength + " allowNull: " + allowNull + " canonicalize: " + canonicalize + " validationErrorList: " + errorList);
		}
		
		String toValidate = (input == null) ? input : input.trim();
		return super.getValidInput(context, toValidate, type, maxLength, allowNull, canonicalize, errorList);
	}
	
	@Override
	public byte[] getValidFileContent(String context, byte[] input, int maxBytes, boolean allowNull) throws ValidationException, IntrusionException {
		
		if (logger.isDebugEnabled()) {
			logger.debug("getValidFileContent: " + context, input, maxBytes, allowNull);
		}
		
		return super.getValidFileContent(context, input, maxBytes, allowNull);
		
	}
	
}
