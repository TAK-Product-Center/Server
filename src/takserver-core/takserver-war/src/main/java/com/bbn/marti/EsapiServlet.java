

package com.bbn.marti;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.SecurityUtils;

/**
 * Wrapper for the <code>HttpServlet</code> class that performs custom servlet initialization.
 * The class is named EsapiServlet because the first customized behavior was to initialize the
 * ESAPI Validator for input validation.
 * 
 * The actual Validator is constructed using the factory method in SecurityUtils.
 * 
 * @see SecurityUtils
 * 
 *
 */
public abstract class EsapiServlet extends HttpServlet {

	private static final long serialVersionUID = 6177006692927399419L;
	
	@Autowired
	protected Validator validator;

	protected Logger log;	

	public EsapiServlet() {
		super();
		log = Logger.getLogger(EsapiServlet.class.getName());
	}

	/**
	 * Initializes the servlet, in this case by parsing <code>context.xml</code> from the global Tomcat configuration
	 * @throws ServletException 
	 */
	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext()); 
		this.initalizeEsapiServlet();
	}

	protected abstract void initalizeEsapiServlet();

	protected Map<String, String[]> validateParams(String context, 
			HttpServletRequest request, HttpServletResponse response,
			Map<String, HttpParameterConstraints> requiredHttpParameters, 
			Map<String, HttpParameterConstraints> optionalHttpParameters) 
					throws ServletException, IOException {

		Map<String, String[]> httpParameters = request.getParameterMap();

		String parameterName = null;
		try {
			validator.assertValidHTTPRequestParameterSet(context, request, requiredHttpParameters.keySet(), 
					optionalHttpParameters.keySet());
			Enumeration<String> parameters = request.getParameterNames();
			while (parameters.hasMoreElements()) {
				parameterName = parameters.nextElement();
				boolean required = true;
				HttpParameterConstraints parameter = requiredHttpParameters.get(parameterName);
				if(parameter == null) {
					parameter = optionalHttpParameters.get(parameterName);
					required = false;
				}
				validator.getValidInput(context, request.getParameter(parameterName),
						parameter.validationPattern.name(), parameter.maximumLength, !required);
			}
			log.finer("HTTP request parameters passed ESAPI validation.");
		} catch (ValidationException ex) {
			String message = "Bad value for HTTP request parameter ";
			if (validator.isValidInput(context, parameterName, "MartiSafeString",
					MartiValidator.SHORT_STRING_CHARS, false)) {
				message = message + "\"" + parameterName + "\"";
			}
			log.warning(message);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return null;
		} catch (IntrusionException ex) {
			String message = "Bad value for HTTP request parameter ";
			if (validator.isValidInput(context, parameterName, "MartiSafeString",
					MartiValidator.SHORT_STRING_CHARS, false)) {
				message = message + "\"" + parameterName + "\"";
			}
			log.severe("Intrusion attempt detected! " + message);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return null;
		} catch (IllegalArgumentException ex) {
			String message = "Unrecognized HTTP request parameter ";
			if (validator.isValidInput(context, parameterName, "MartiSafeString",
					MartiValidator.SHORT_STRING_CHARS, false)) {
				message = message + "\"" + parameterName + "\"";
			}
			log.warning(message);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return null;
		}

		return httpParameters;
	}

	/**
	 * Gets the value of the HTTP request parameter matching the desired <code>given</code>.
	 * Specifically, performs a case-insensitive match of the given <code>String</code> against the
	 * HTTP parameter keys and returns the first value of for that key. The <code>HttpServletRequest</code> API
	 * supports multi-valued parameters, but this function does not (and neither does Tomcat).
	 * 
	 * @param httpParameters Map of the HTTP request parameters as returned by <code>HttpServletRequest.getParameterMap</code>
	 * @param given parameter whose value to look up.
	 * @return first value of the matching HTTP parameter, or <code>null</code> if no match is found.
	 */
	public String getParameterValue(Map<String, String[]> httpParameters, String given) {
		String result = null;
		for (String key : httpParameters.keySet()) {
			if (key.compareToIgnoreCase(given) == 0) {
				String[] values = httpParameters.get(key);
				String safeKey = key;
				if (!validator.isValidInput("getParameterValue", key, "MartiSafeString", 
						MartiValidator.DEFAULT_STRING_CHARS, true)) {
					safeKey = "(unsafe parameter name)";
				}
				if (values.length > 1) {
					log.warning("Multiple values detected for HTTP parameter " + StringUtils.normalizeSpace(safeKey) + "; ignoring extras." );
				} 
				if (!values[0].isEmpty()) {
					result = values[0];
				}
			}
		}
		return result;
	}

	protected void initAuditLog(HttpServletRequest request) {
	    if (request == null) {
	        log.warning("null HttpServletRequest in EsapiServlet");
	    }
	    AuditLogUtil.init(request);
	}
}
