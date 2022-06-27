<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@page import="java.util.logging.Logger"%>
<%@page import="com.bbn.marti.logging.AuditLogUtil" %>
<%@page import="java.security.Principal" %>

<%
    AuditLogUtil.init(request);
%>
