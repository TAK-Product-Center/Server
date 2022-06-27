<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>Data Ferry Files</title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<%@ page import="java.rmi.Naming" %>
		<%@ page import="java.net.URI" %>
 		<%@ page import="java.util.List" %>
		<%@ page import="java.util.Map" %>
		<%@ page import="java.util.Set" %>
		<%@ page import="java.util.LinkedList" %>
		<%@ page import="java.util.Collection" %>
		<%@ page import="java.util.Collections" %>
		<%@ page import="com.bbn.marti.FileDownloadCounter" %>
		<%@ page import="com.bbn.marti.remote.ContactManager" %>
		<%@ page import="com.bbn.marti.remote.RemoteFile" %>
		
		<link rel="stylesheet" href="tablesorter/style.css" type="text/css" media="print, projection, screen" />
		<link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
		<link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
		<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
		<script type="text/javascript" src="jquery/jquery-ui.js"></script> 
		<script type="text/javascript" src="tablesorter/jquery.tablesorter.min.js"></script> 
		<script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
		<script type="text/javascript">
			$(document).ready(function() 
				{ 
					$("#myTable").tablesorter(); 
				}
			);
		</script>
		<%!public String tableHead() 
		{
			StringBuilder sb = new StringBuilder("<thead><tr>");
			sb.append("<th>Filename</th>");
			sb.append("<th>Downloaded by</th>");
			sb.append("</tr></thead>");
			return sb.toString();
		}
		public String fileToRow(RemoteFile file)
		{
			List<String> downloadersList = FileDownloadCounter.instance().getDownloaders(file.filename);
			String downloaders = downloadersList != null ? downloadersList.toString() : "(none)";

			StringBuilder sb = new StringBuilder("<tr valign=\"top\">");
			sb.append(" <td> <a href='GetFile?file=" + file.filename + "'>" + file.filename + "</a></td> ");
			sb.append(" <td> " + downloaders + " </td> ");
    		sb.append("</tr>");
			return sb.toString();
		}%>

	</head>

	<body>	
		<%@ include file="menubar.html" %>
		<%@ include file="init_audit_log.jsp" %>
		
		<h3>Data Ferry Files</h3>
		<%
			ContactManager contactMgr = (ContactManager) Naming
						.lookup("//127.0.0.1:3334/ContactManager");
				
				out.print("<table id=\"myTable\" class=\"tablesorter\"> ");
				out.print(tableHead());
				out.print("<tbody>");
				
				for (RemoteFile file : contactMgr.getFileList()) {
					if(file != null) {
						out.print(fileToRow(file));
					}
				}
				out.print("</tbody></table>");
		%>
		<%@ include file="footer.jsp" %>
		
	</body>
</html>
