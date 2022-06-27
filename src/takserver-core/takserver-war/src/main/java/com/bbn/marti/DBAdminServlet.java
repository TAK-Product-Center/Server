

package com.bbn.marti;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class DBAdminServlet extends EsapiServlet {

	private static final long serialVersionUID = 2933929482750265895L;
	private Logger logger = LoggerFactory.getLogger(DBAdminServlet.class);

	@Autowired
	private JDBCQueryAuditLogHelper dbLogHelper;

	@Autowired
	private DataSource ds;

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sql = null;

		initAuditLog(request);

		for(String s : request.getParameterMap().keySet()) {
			if(s.compareToIgnoreCase("vacuum") == 0) {
				sql = "VACUUM";
				break;
			} else if(s.compareToIgnoreCase("reindex") == 0) {
				sql = "REINDEX DATABASE cot";
				break;
			}
		}

		if(sql == null) {

			String msg = "Invalid parameters";

			logger.error(msg);

			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Executing database operation: " + sql);
		}

		try (Connection connection = ds.getConnection(); PreparedStatement ps = dbLogHelper.prepareStatement(sql, connection)) {
			dbLogHelper.doUpdate(ps);

			if (logger.isDebugEnabled()) {
				logger.debug("Completed database operation: " + sql);
			}
		} catch (SQLException | NamingException e) {

			logger.error("error executing DB admin operation", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
			return;
		}
		response.getWriter().print("Success");
	}

    @Override
    protected void initalizeEsapiServlet() { }

}
