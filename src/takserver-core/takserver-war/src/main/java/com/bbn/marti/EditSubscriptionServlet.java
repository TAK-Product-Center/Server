

package com.bbn.marti;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.remote.SubscriptionManagerLite;

//@WebServlet("/EditSubscriptionServlet")
public class EditSubscriptionServlet extends EsapiServlet {

	public static final String UID_KEY = "uid";
	public static final String XPATH_KEY = "xpath";
	
	private static final long serialVersionUID = 3175569118632800070L;
	
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Resets the XPath of an existing subscription.
	 * Required HTTP parameters: 
	 *    uid - UID of the subscription to update
	 *    xpath - revised XPath for that subscription.
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	    
	    initAuditLog(request);
	    
		String uid = request.getParameter(UID_KEY);
		String xpath = request.getParameter(XPATH_KEY);
		if (uid == null || xpath == null || uid.isEmpty() || xpath.isEmpty()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"Missing HTTP parameter. \"" + UID_KEY + "\" and \"" + XPATH_KEY + "\" are required.");
		}
		
		try {
	         SubscriptionManagerLite subMgr = (SubscriptionManagerLite) Naming
	            .lookup("//127.0.0.1:3334/SubMgr");
	         subMgr.setXpathForUid(uid, xpath);
	         response.setStatus(HttpServletResponse.SC_OK);
		} catch (IllegalArgumentException iae) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "XPath argument failed validation check.");
		} catch (RemoteException ri) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Runtime error executing RMI call.");
			ri.printStackTrace();
		} catch (NotBoundException nbe) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
					"RMI lookup for Subscription Manager failed.");
			nbe.printStackTrace();
		} 
		
		response.getWriter().write("<html><body></body></html>");
	}

    @Override
    protected void initalizeEsapiServlet() { }	
}
