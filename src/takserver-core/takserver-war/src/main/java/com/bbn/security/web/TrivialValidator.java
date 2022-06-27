

package com.bbn.security.web;

import org.owasp.esapi.Encoder;
import org.owasp.esapi.ValidationErrorList;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;

/**
 * Class that implements the Validator API, but does not enforce any user-defined validation rules. 
 * For use in environments where security is disabled.
 * Certain methods, such as <code>getValidDate</code> and <code>getValidInteger</code>, use 
 * well-known validation patterns and so inherit their implementations from 
 * <code>org.owasp.esapi.reference.DefaultValidator</code>.
 * 
 *
 */
public class TrivialValidator extends MartiValidator {
	
	public static Validator getInstance() {
		if (TrivialValidator.INSTANCE == null) {
			INSTANCE = new TrivialValidator();
		}
		return INSTANCE;
	}
	
	private static TrivialValidator INSTANCE = null;
	
	public TrivialValidator() {
		super();
	}
	
	public TrivialValidator(Encoder encoder) {
		super(encoder);
	}
	
	@Override
	public void assertValidFileUpload(java.lang.String context,
            java.lang.String directorypath,
            java.lang.String filename,
            java.io.File parent,
            byte[] content,
            int maxBytes,
            java.util.List<java.lang.String> allowedExtensions,
            boolean allowNull)
     throws ValidationException,
            IntrusionException {
		// Do nothing. Throw no exceptions.
	}
	
	@Override
	public void assertValidFileUpload(java.lang.String context,
            java.lang.String filepath,
            java.lang.String filename,
            java.io.File parent,
            byte[] content,
            int maxBytes,
            java.util.List<java.lang.String> allowedExtensions,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		// Do nothing. Throw no exceptions.
	}
	
	@Override
	public void assertValidHTTPRequestParameterSet(java.lang.String context,
            javax.servlet.http.HttpServletRequest request,
            java.util.Set<java.lang.String> required,
            java.util.Set<java.lang.String> optional,
            ValidationErrorList errors)
     throws IntrusionException {
		// Do nothing
	}
	
	public java.lang.String getValidCreditCard(java.lang.String context,
             java.lang.String input,
             boolean allowNull)
      throws ValidationException,
             IntrusionException {
		 return input;
	 }
	
	public java.lang.String getValidCreditCard(java.lang.String context,
             java.lang.String input,
             boolean allowNull,
             ValidationErrorList errors)
      throws IntrusionException {
		 return input;
	 }
	
	/**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	public java.lang.String getValidDirectoryPath(java.lang.String context,
            java.lang.String input,
            java.io.File parent,
            boolean allowNull)
     throws ValidationException,
            IntrusionException {
		return input;
	}
	
	/**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	public java.lang.String getValidDirectoryPath(java.lang.String context,
            java.lang.String input,
            java.io.File parent,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return input;
	}
	

	@Override
	public byte[] getValidFileContent(java.lang.String context,
            byte[] input,
            int maxBytes,
            boolean allowNull)
     throws ValidationException,
            IntrusionException {
		return input;
	}
	
	@Override
	public java.lang.String getValidFileName(java.lang.String context,
            java.lang.String input,
            java.util.List<java.lang.String> allowedExtensions,
            boolean allowNull)
     throws ValidationException,
            IntrusionException {
		return input;
	}
	
	@Override
	public java.lang.String getValidFileName(java.lang.String context,
            java.lang.String input,
            java.util.List<java.lang.String> allowedParameters,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return input;
	}
	
	/**
	 * Always returns the parameter "input," unmodified. Never throws exceptions.
	 */
	@Override
	 public java.lang.String getValidInput(java.lang.String context,
	                                       java.lang.String input,
	                                       java.lang.String type,
	                                       int maxLength,
	                                       boolean allowNull)
	                                throws ValidationException {
		 return input;
	 }
	
	/**
	 * Always returns the parameter "input," unmodified. Never throws exceptions.
	 */
	@Override
	public java.lang.String getValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            boolean canonicalize)
     throws ValidationException {
		return input;
	}
	
	/**
	 * Always returns the parameter "input," unmodified. Never throws exceptions.
	 */
	@Override
	public java.lang.String getValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            boolean canonicalize,
            ValidationErrorList errors)
     throws IntrusionException {
		return input;
	}
	
	// getValidDate uses parent's implementation
	
	/**
	 * Always returns the parameter "input," unmodified. Never throws exceptions.
	 */
	@Override
	public java.lang.String getValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return input;
	}
	
	/**
	 * Always returns the input. Never throws exceptions.
	 */
	@Override
	public java.lang.String getValidSafeHTML(java.lang.String context,
            java.lang.String input,
            int maxLength,
            boolean allowNull)
     throws ValidationException,
            IntrusionException {
		return input;
	}
	
	/**
	 * Always returns the input. Never throws exceptions.
	 */
	public java.lang.String getValidSafeHTML(java.lang.String context,
            java.lang.String input,
            int maxLength,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return input;
	}
	
	/**
	 * Always returns true. Never throws exceptions.
	 */
	 public boolean isValidCreditCard(java.lang.String context,
	                                  java.lang.String input,
	                                  boolean allowNull)
	                           throws IntrusionException {
		 return true;
	 }
	
	 /**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidDate(java.lang.String context,
            java.lang.String input,
            java.text.DateFormat format,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}
	 
	 /**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidDate(java.lang.String context,
            java.lang.String input,
            java.text.DateFormat format,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	 
	 public boolean isValidDirectoryPath(java.lang.String context,
             java.lang.String input,
             java.io.File parent,
             boolean allowNull)
      throws IntrusionException {
		 return true;
	 }
	 
	
	 /**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidDirectoryPath(java.lang.String context,
            java.lang.String input,
            java.io.File parent,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	 
	 @Override
	public boolean isValidFileContent(java.lang.String context,
            byte[] input,
            int maxBytes,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}

	 @Override
	public boolean isValidFileContent(java.lang.String context,
            byte[] input,
            int maxBytes,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	
	 /**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	public boolean isValidFileName(java.lang.String context,
            java.lang.String input,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}
	
	 /**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	 public boolean isValidFileName(java.lang.String context,
	                                java.lang.String input,
	                                boolean allowNull,
	                                ValidationErrorList errors)
	                         throws IntrusionException {
		 return true;
	 }
	 
	 /**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	public boolean isValidFileName(java.lang.String context,
            java.lang.String input,
            java.util.List<java.lang.String> allowedExtensions,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}
	
	 /**
	  * Always returns true. Never throws exceptions.
	  */
	@Override
	public boolean isValidFileName(java.lang.String context,
            java.lang.String input,
            java.util.List<java.lang.String> allowedExtensions,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	
	 @Override 
	public boolean isValidFileUpload(java.lang.String context,
            java.lang.String directorypath,
            java.lang.String filename,
            java.io.File parent,
            byte[] content,
            int maxBytes,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}
	
	
	@Override
	public boolean isValidFileUpload(java.lang.String context,
            java.lang.String directorypath,
            java.lang.String filename,
            java.io.File parent,
            byte[] content,
            int maxBytes,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	
	@Override
	public boolean isValidHTTPRequestParameterSet(java.lang.String context,
            javax.servlet.http.HttpServletRequest request,
            java.util.Set<java.lang.String> requiredNames,
            java.util.Set<java.lang.String> optionalNames) {
		return true;
	}

	// isValidNumber and getValidNumber use base class's implementation
	
	// isValidDouble and getValidDouble use base class's implementation
	
	@Override
	public boolean isValidHTTPRequestParameterSet(java.lang.String context,
            javax.servlet.http.HttpServletRequest request,
            java.util.Set<java.lang.String> requiredNames,
            java.util.Set<java.lang.String> optionalNames,
            ValidationErrorList errors) {
		return true;
	}

	/**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            boolean canonicalize)
     throws IntrusionException {
		return true;
	}
 
	/**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            boolean canonicalize,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	
	/**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidInput(java.lang.String context,
            java.lang.String input,
            java.lang.String type,
            int maxLength,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}
	
	/**
	 * Always returns true
	 */
	@Override 
	public boolean isValidInput(String context, String input, String type, int maxLength, boolean allowNull)
	throws IntrusionException {
		return true;
	}
	
	// isValidListItem and getValidListItem use base implementation
	
	/**
	 * Always returns true. Never throws exceptions.
	 */
	@Override
	public boolean isValidSafeHTML(java.lang.String context,
            java.lang.String input,
            int maxLength,
            boolean allowNull)
     throws IntrusionException {
		return true;
	}
	
	/**
		 * Always returns true. Never throws exceptions.
		 */
	@Override
	public boolean isValidSafeHTML(java.lang.String context,
            java.lang.String input,
            int maxLength,
            boolean allowNull,
            ValidationErrorList errors)
     throws IntrusionException {
		return true;
	}

	// isValidPrintable and getValidPrintable use base implementation
	
	
}
