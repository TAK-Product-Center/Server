

package com.bbn.marti;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.security.web.MartiValidator;

public class ResubscribeServlet extends EsapiServlet {
	private static final long serialVersionUID = 8695971270050381226L;
	
	@Autowired
	private JDBCQueryAuditLogHelper wrap;
	
	@Autowired
	private DataSource ds;

	public ResubscribeServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
				"GET is not supported by ResubscribeServlet");
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Map<String, String[]> params = new HashMap<String, String[]>(
				request.getParameterMap());
		String context = "ResubscribeServlet";

		initAuditLog(request);

		// create subscriptions for the other parameters
		for (String id : params.keySet()) {
			removeSubscription(id);
		}

		// re-direct back to SubMgr
		String referer = request.getHeader("referer");

		try {
			referer = validator.getValidInput(context, referer, "URL", MartiValidator.LONG_STRING_CHARS, false);
		} catch (ValidationException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Bad value for parameter \"referer\":" + e.getMessage());
			return;
		} catch (IntrusionException e) {
			log.severe("Intrusion attempt detected" + e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Bad value for parameter \"referer\":" + e.getMessage());
			return;
		}

		response.sendRedirect(referer);
	}

	private void removeSubscription(String id) {
		try (Connection connection = ds.getConnection(); PreparedStatement sqlQuery = wrap
					.prepareStatement("DELETE FROM subscriptions WHERE id=?", connection)) {
			int idInt = Integer.parseInt(id);
			if (idInt < 0) {
				throw new IllegalArgumentException(
						"Subscription ID cannot be negative");
			}
			sqlQuery.setInt(1, idInt);
			wrap.doUpdate(sqlQuery);
		} catch (Exception e) {
			// not sure where this output goes
			// but we're just doing best effort here...
			e.printStackTrace();
		}
	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(ResubscribeServlet.class.getCanonicalName());		
	}
}
