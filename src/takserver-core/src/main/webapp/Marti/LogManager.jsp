<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="com.bbn.marti.util.spring.SpringContextBeanForApi" %>
<%@ page import="com.bbn.marti.logs.Log" %>
<%@ page import="com.bbn.marti.logs.PersistenceStore" %>
<%@ page import="org.owasp.esapi.ESAPI" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache"> 
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <title>Log Manager</title>
    <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
    <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
    <link rel="stylesheet" href="css/bootstrap-theme.min.css" />
    <link rel="stylesheet" href="css/bootstrap.min.css">
    <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
    <script type="text/javascript" src="jquery/jquery-ui.js"></script> 
    <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
</head>
<body>
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

<%
	Boolean showUnique = request.getParameter("showUnique") != null && request.getParameter("showUnique").compareTo("true") == 0;
	Boolean showMetrics = request.getParameter("metrics") != null && request.getParameter("metrics").compareTo("true") == 0;
	Boolean showErrorLogs = !showUnique && !showMetrics;
%>

<script language="javascript" type="text/javascript">

	function doDelete(id) {

		$.ajax({
			url  : "ErrorLog?id=" + id,
			type : "DELETE",
			async : false,
			cache : false,
			contentType : false,
			processData : false,
			error : function(stat, err) {
				$.jnotify("Error deleting", "error");
			}
		});

	}

	function deleteErrorLog(id) {
		
		if (!confirm("Press Ok to confirm deletion")) {
			return;
		}		

		doDelete(id);

    	location.reload();
	}

	function getSelected() {
		var selected = [];
		$('input[type=checkbox]').each(function(){
				if (this.checked) {
					selected.push(this.value);
				}
			});
		
		return selected;
	}
  
  	function selectAll() {
		$('input[type=checkbox]').each(function(){
			this.checked = !this.checked;
		});
  	}

    function selectGroup(groups) {
        $('input[type=checkbox]').each(function(){
            if (groups.includes(this.id)) {
                this.checked = !this.checked;
            }
        });
    }

    function selectId(checkbox, id) {
        $('input[type=checkbox]').each(function(){
            if (checkbox != this && id == this.id) {
                this.checked = !this.checked;
            }
        });
    }

	function deleteSelected() {

		if (!confirm("Press Ok to confirm deletion")) {
			return;
		}

		var selected = getSelected();
		if (selected.length == 0) {
			return;
		}
		
    	var ids = "";
		for (var i=1; i<=selected.length; i++) {
			ids += selected[i-1] + ",";

			if (i % 50 == 0) {
				doDelete(ids);
				ids = "";
			}
		}    
		
		doDelete(ids);

		location.reload();
	}

	function downloadSelected() {
		var selected = getSelected();
		if (selected.length == 0) {
			return;
		}
    
    	var ids = "";
		for (var i=0; i<selected.length; i++) {
			ids += selected[i] + ",";
		}
		
		window.open("ErrorLog?id=" + ids);
	}

	function downloadAll() {
		window.open("ErrorLog?id=ALL");
	}

	function searchErrorLogs(query) {
	    var separator = "?";
        var location = window.location.href;

        var queryStart = location.indexOf("query=");
        if (queryStart >=0) {
            location = location.substr(0, queryStart-1);
        }

        if (location.indexOf('?') >= 0) { separator = '&'; }

        location =  location + separator + 'query=' + document.getElementById('query').value;
		window.location = location;
    }

</script>

<h1>Log Manager</h1>

<table border=0 width=100%>
	<tr>
		<td align=left>
			<input type="radio" id="setLogView" value="Show All" onClick="javascript:window.location = 'LogManager.jsp';" <%if (showErrorLogs) {%> checked <%}%> />Error Logs&nbsp;
		<!--<input type="radio" id="setLogView" value="Group By Callstack" onClick="javascript:window.location = 'LogManager.jsp?showUnique=true';" <% if (showUnique) { %> checked <% } %> />Error Logs Grouped By Callstack&nbsp;-->
			<input type="radio" id="setLogView" value="Show Metrics" onClick="javascript:window.location = 'LogManager.jsp?metrics=true';" <%if (showMetrics) {%> checked <%}%> />Metrics&nbsp;
		</td>
		<td align=right>
			<input type="text" id="query" name="query" size=40 value="<%=ESAPI.encoder().encodeForHTML(request.getParameter("query")) == null ? "" : ESAPI.encoder().encodeForHTML(request.getParameter("query"))%>"/>&nbsp;
			<input type="button" id="searchButton" value="Search" onClick="searchErrorLogs(document.getElementById('query').value);" />
		</td>
</table>

<br><br>

	<%
		int size = 0;
			HashMap<String, List<Log>> uniques = null;
			List<Log> logs = null;

			if (showUnique) {
		uniques = SpringContextBeanForApi.getSpringContext()
				.getBean(PersistenceStore.class).getUniqueErrorLogs(request.getParameter("query"));
		size = uniques.size();
			} else if (showMetrics){
		logs = SpringContextBeanForApi.getSpringContext()
				.getBean(PersistenceStore.class).getLogs(request.getParameter("query"), true, false, false);
		size = logs.size();
			} else {
		logs = SpringContextBeanForApi.getSpringContext()
				.getBean(PersistenceStore.class).getLogs(request.getParameter("query"), false, false, false);
		size = logs.size();
			}
	%>

<input type="button" id="selectAll" value="Select all" onClick="javascript:selectAll();"/>&nbsp;
<input type="button" id="downloadSelected" value="Download Selected" onClick="javascript:downloadSelected();"/>&nbsp;
<input type="button" id="downloadAll" value="Download All" onClick="javascript:downloadAll();"/>&nbsp;
<input type="button" id="deleteSelected" value="Delete Selected" onClick="javascript:deleteSelected();"/>&nbsp;
<br><br>

<%=size%>
<%
	if (showUnique) {
%>
	Unique Callstacks
<%
	} else if (showMetrics) {
%>
	Metrics Logs
<%
	} else {
%>
	Error Logs <%
	}
%>

<form id="errorLogForm">

<table border=1>
<tr>
<%=showUnique ? "<th>Callstack</th>" : ""%>
  <th>Select</th>
<%=showUnique ? "<th>Exception</th>" : ""%>
  <th>Upload Time</th>
  <th>UID</th>
  <th>Callsign</th>
  <th>Platform</th>
  <th>Major Version</th>
  <th>Minor Version</th>
  <th>Filename</th>
  <th>Action</th>
</tr>

<%
	if (showUnique) {
		for (Map.Entry<String, List<Log>> unique : uniques.entrySet()) {

	String callstack = ESAPI.encoder().encodeForHTML(unique.getKey());
	logs = unique.getValue();
	boolean first = true;

	String groupIds = "[";
	for (Log log : logs) {
		groupIds += log.getId() + ",";
	}
	groupIds += "]";
	groupIds = ESAPI.encoder().encodeForHTML(groupIds);

	for (Log log : logs) {
%>
				<tr>
					<%=first ? "<td width=50% rowspan=\"" + logs.size() + "\">"
							+ "<input type=\"button\" id=\"selectGroupz\" value=\"Select Group (" + logs.size() + ")\" onClick=\"selectGroup('" + groupIds + "');\"/><br><br>"
							+ callstack  + "</td>" : ""%>

					<td align=center><input type="checkbox" name="errorlogs" value="<%=log.getId()%>"
											id="<%=log.getId()%>" onClick="javascript:selectId(this, <%=log.getId()%>);"
											autocomplete="off"/></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getException(callstack))%></td>
					<td><%=log.getTime().toLocalDateTime().toString()%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getUid())%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getCallsign())%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getPlatform())%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getMajorVersion())%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getMinorVersion())%></td>
					<td><%=ESAPI.encoder().encodeForHTML(log.getFilename())%></td>
					<td>
						<input type="button" id="download" value="Download" onClick="javascript:window.open('ErrorLog?id=<%=log.getId()%>');"/>
						&nbsp;
						<input type="button" id="delete" value="Delete" onClick="javascript:deleteErrorLog('<%=log.getId()%>');"/>
					</td>
				</tr>
<%
	first = false;
	}
		}

	} else {
	    for (Log log : logs) {
%>
		<tr>

			<td align=center><input type="checkbox" name="errorlogs" value="<%=log.getId()%>" autocomplete="off"/></td>
			<td><%=log.getTime().toLocalDateTime().toString()%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getUid())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getCallsign())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getPlatform())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getMajorVersion())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getMinorVersion())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(log.getFilename())%></td>
			<td>
				<input type="button" id="download" value="Download" onClick="javascript:window.open('ErrorLog?id=<%=log.getId()%>');"/>
				&nbsp;
				<input type="button" id="delete" value="Delete" onClick="javascript:deleteErrorLog('<%=log.getId()%>');"/>
			</td>
		</tr>
<%
	}
	}
%>

</table>
</form>

<hr />
	<%@ include file="footer.jsp" %>
</body>
</html>
