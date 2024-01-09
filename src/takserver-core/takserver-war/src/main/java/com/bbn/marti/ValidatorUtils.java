package com.bbn.marti;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bbn.security.web.MartiValidatorConstants;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;


public class ValidatorUtils {
	
	/**
	 * Referenced implementation: https://github.com/ESAPI/esapi-java-legacy/blob/develop/src/main/java/org/owasp/esapi/reference/DefaultValidator.java
	 * @param context
	 * @param requestParameterKeySet
	 * @param required
	 * @param optional
	 * @throws IntrusionException
	 * @throws ValidationException
	 */
	public static void assertValidHTTPRequestParameterSet(
	        String context, Set<String> requestParameterKeySet, Set<String> required, Set<String> optional,
            Validator validator) throws IntrusionException, ValidationException {

        Set<String> given = requestParameterKeySet;
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
                if (validator.isValidInput("assertValidHttpRequestParameterSet", badExtra, "HTTPParameterName",
                        MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
                    badParameters.append(badExtra);
                } else {
                    badParameters.append("[redacted]");
                }
                if (extraItr.hasNext()) {
                    badParameters.append(", ");
                }
            }

            throw new ValidationException(context + ": Invalid HTTP request extra parameters (redacted) ",
                    "Invalid HTTP request extra parameters " + badParameters.toString() + ": context=" + context, context);
        }
	}
}
