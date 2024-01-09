

package com.bbn.marti;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;

import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Servlet implementation class CreateSubscriptionServlet
 */
//@WebServlet("/CreateSubscriptionServlet")
public class CreateSubscriptionServlet extends EsapiServlet {
	
	private static final long serialVersionUID = 4136211855760604133L;
	public static final String CONTEXT = "CreateSubscriptionServlet";

	@Autowired
	private SubscriptionManagerLite subMgr;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CreateSubscriptionServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
				"GET is not supported by CreateSubscriptionServlet");
	}

	/**
	 * Hard coded values
	 */
	private static final String addSubType = "t-b";
	private static final String delSubType = "t-b-c";

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 *      Based largely on CotSubscriptionHelper
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String connType;
		String subscriberAddress;
		int subscriberPort;
		String xpathString;

		/**
		 * Values parsed from request
		 */
		
		initAuditLog(request);
		
		String uid = request.getParameter("uid").trim();
		if (validator != null && !validator.isValidInput(CONTEXT, uid, "MartiSafeString", 
				MartiValidatorConstants.DEFAULT_STRING_CHARS, false) || uid.isEmpty()) {
				String message = "Bad value for request parameter \"uid\"";
				log.severe(message);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
		}
		
		String deleteString = request.getParameter("delete");
		boolean delete = false;
		if (deleteString != null
				&& deleteString.trim().equalsIgnoreCase("delete")) {
			delete = true;
		}

		if (delete) {
			subMgr.deleteSubscription(uid);
			response.getWriter().print(
					"Deleted subscription: \n\n + " + ESAPI.encoder().encodeForHTML(uid) + "\n");
		} else {
			connType = request.getParameter("prot").trim();;
			subscriberAddress = request.getParameter("subaddr").trim();
			xpathString = request.getParameter("xpath").trim();
			subscriberPort = -1; // Will be overwritten immediately in the next block
			try {
				// Check XPath can be compiled first because that throws the most informative exceptions
				if (xpathString != null && !xpathString.isEmpty()) {
					xpathString = validator.getValidInput(CONTEXT, xpathString, "XpathBlackList",
							2048, true);
					XPath xpath = XPathFactory.newInstance().newXPath();
					xpath.compile(xpathString);
				}
				
				connType = validator.getValidInput(CONTEXT, connType, "SupportedProtocol", 
							MartiValidatorConstants.SHORT_STRING_CHARS, false);
				subscriberAddress = validator.getValidInput(CONTEXT, subscriberAddress, "MartiSafeString", 
							MartiValidatorConstants.DEFAULT_STRING_CHARS, false);

				subscriberPort = Integer.parseInt(request.getParameter("subport"));
				if (subscriberPort < 0 || subscriberPort > 65535) {
					throw new IllegalArgumentException("port number " + subscriberPort + " is out of range.");
				}

				RemoteSubscription sub = new RemoteSubscription();
				sub.uid = uid;
				sub.clientUid = uid;
				sub.to = connType + ":" + subscriberAddress + ":" + subscriberPort;
				sub.xpath = xpathString;
				subMgr.addSubscription(sub);
				response.getWriter().print(
						"Added subscription: \n\n" + ESAPI.encoder().encodeForHTML(uid));
			} catch (XPathExpressionException ex) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid XPath");
				return;
			} catch (IllegalArgumentException ex) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
						"Bad value for request parameter \"subport\": " + ex.getMessage());
				return;
			} catch (ValidationException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
						"Bad value for request parameter: " + e.getMessage());
				log.warning(e.getMessage());
				return;
			} catch (IntrusionException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
						"Bad value for request parameter: " + e.getMessage());
				log.severe(e.getMessage());
				return;
			}
		}
	}

	@Override
	protected void initalizeEsapiServlet() {
		 this.log = Logger.getLogger(CreateSubscriptionServlet.class.getCanonicalName());		
	}

}
