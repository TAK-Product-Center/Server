

package com.bbn.marti;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Servlet implementation class WriteImageServlet
 */
//@WebServlet("/GetCotData")
public class GetCotDataServlet extends EsapiServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(GetCotDataServlet.class);

	private static final long serialVersionUID = -1643155275297691951L;
	
	@Autowired
	private JDBCQueryAuditLogHelper queryWrapper;
	
	@Autowired
	private DataSource ds;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetCotDataServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	    
	    initAuditLog(request);
	    
		int cotId = -1;
		String cotUid = null;
		boolean isXml = false;
		
		try {
			cotId = Integer.parseInt(request.getParameter("cotId"));
		} catch (Exception e) { }	
			
		try {
			cotUid = request.getParameter("uid");
		} catch (Exception e) { }
		
		try {
			isXml = request.getParameter("xml") != null;
		} catch (Exception e) { }		
		
		if (cotId < 1 && cotUid == null) {
		    
		    String msg = "either uid or cotId must be specified as a request parameter";
		    
		    response.sendError(400, msg);
		    
			logger.warn(msg);

			return;
		}
		
		Document doc = null;

		// query by cot uid
		if (cotUid != null) {
		    // get latest cot event by uid
		    String cotQuery = "SELECT id, uid, cot_type, access, qos, opex, start, time, stale, how, point_hae, point_ce, point_le, detail, servertime, event_pt, ST_AsText(event_pt) FROM cot_router WHERE uid = ? ORDER BY id DESC LIMIT 1";
		    try {
		    	try (Connection connection = ds.getConnection(); PreparedStatement stmt = queryWrapper.prepareStatement(cotQuery, connection)) {
		    		stmt.setString(1, cotUid);

		    		try (ResultSet results = queryWrapper.doQuery(stmt)) {

		    			if (results.next() == false) {
		    				response.sendError(404);
		    				return;
		    			}
		    			doc = CotImageBean.buildCot(results);
		    		}
		    	}

		    } catch (Exception e) {
		        logger.warn("exception executing CoT query " + e.getMessage(), e);
		    }

		} else if (cotId >= 0) {
		    // query DB for CoT meta-data on cotId
		    String cotQuery = "SELECT id, uid, cot_type, access, qos, opex, start, time, stale, how, point_hae, point_ce, point_le, detail, servertime, event_pt, ST_AsText(event_pt) FROM cot_router WHERE id = ?";
		    try (Connection connection = ds.getConnection(); PreparedStatement stmt = queryWrapper.prepareStatement(cotQuery, connection)) {
		        stmt.setInt(1, cotId);
		        
		        try (ResultSet results = queryWrapper.doQuery(stmt)) {

		        	if (results.next() == false) {
		        		response.sendError(404);
		        		return;
		        	}

		        	doc = CotImageBean.buildCot(results);
		        }

		    } catch (Exception e) {
		        logger.warn("exception executing CoT query " + e.getMessage(), e);
		    }
		}

		// respond with XML
		if (isXml) {
		    response.setContentType("application/xml");
		    response.getWriter().write(doc.asXML());
		    return;
		}

		// respond with JSON
		response.setContentType("application/json");
		response.getWriter().write(buildJson(doc));
	}

	private String buildJson(Document doc) {
		String ret = "{ ";
		if (doc.getRootElement().element("detail") != null
				&& doc.getRootElement().element("detail").element("remarks") != null
				&& doc.getRootElement().element("detail").element("remarks")
						.getText() != null) {
			ret += " \"remarks\" : \""
					+ doc.getRootElement().element("detail").element("remarks")
							.getText() + "\",";
		}
		ret += " \"uid\" : \"" + doc.getRootElement().attributeValue("uid")
				+ "\",";
		ret += " \"type\" : \"" + doc.getRootElement().attributeValue("type")
				+ "\",";
		ret += " \"how\" : \"" + doc.getRootElement().attributeValue("how")
				+ "\",";
		ret += " \"lat\" : \"" + doc.getRootElement().element("point").attributeValue("lat")
				+ "\",";
		ret += " \"lon\" : \"" + doc.getRootElement().element("point").attributeValue("lon")
				+ "\",";
		ret += " \"hae\" : \"" + doc.getRootElement().element("point").attributeValue("hae")
				+ "\",";
		ret += " \"le\" : \"" + doc.getRootElement().element("point").attributeValue("le")
				+ "\",";
		ret += " \"ce\" : \"" + doc.getRootElement().element("point").attributeValue("ce")
				+ "\" }";
		return ret;
	}

    @Override
    protected void initalizeEsapiServlet() { }

}
