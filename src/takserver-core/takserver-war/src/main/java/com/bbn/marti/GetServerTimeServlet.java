

package com.bbn.marti;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//@WebServlet("/GetTime")
public class GetServerTimeServlet extends EsapiServlet {
	
	private static final long serialVersionUID = -699520042415841411L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	    
	    initAuditLog(request);
	    
		response.setContentType("application/json");
		response.getWriter().write(
				"{ \"time\" : \""+System.currentTimeMillis()+"\" }"
				);
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

    @Override
    protected void initalizeEsapiServlet() { }
}
