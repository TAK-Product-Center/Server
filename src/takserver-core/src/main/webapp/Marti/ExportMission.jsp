<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link rel="icon" type="image/x-icon" href="favicon.ico" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Marti Mission Export</title>

<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
<script type="text/javascript" src="jquery/jquery-ui.js"></script>
<link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
<link rel="stylesheet" href="css/bootstrap-theme.min.css" />
<link rel="stylesheet" href="css/bootstrap.min.css">

<script type="text/javascript" >
	$(document).ready(function() {
		$('#copyKmlButton').click(
			function() {
				$('<div />').html(	
						"${pageContext.request.getScheme()}" + "://" +
						"${pageContext.request.getServerName()}" + ":" +
						"${pageContext.request.getServerPort()}" +
						"${pageContext.request.getContextPath()}" + "/Marti/ExportMissionKML?" +
						"startTime=" + $('#min').val() + "&endTime=" + $('#max').val() + "&interval=" + $('#interval').val() + 
						"&uid=" + $('#uid').val() + '&multiTrackThreshold=' + $('#multiTrackThreshold').val() +
						"&extendedData=" + $('#extendedData').val() +
						"&optimizeExport=" + $('#optimizeExport').val()).dialog({"title": "URL", "width": 800 });
			}
		);
	  }
	);
</script>

<style>
	table[id="queryFields"] tr td:first-child  {
		text-align:right;
	}
</style>
</head>
<body>
<%@ include file="menubar.html"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="java.util.SimpleTimeZone"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.sql.Timestamp"%>
<%@page import="tak.server.Constants"%>
<%@page import="tak.server.util.Association"%>
<%@page import="com.bbn.marti.util.*"%>
<%@page import="com.bbn.marti.util.spring.*"%>
<%@page import="com.bbn.marti.dao.kml.*"%>
<%@page import="com.bbn.marti.remote.groups.GroupManager"%>
<%@page import="com.bbn.marti.remote.groups.Group"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.bbn.marti.remote.util.SpringContextBeanForApi" %>


<h1>Export Mission to KML</h1>
<form name="spatialQueryForm" method="post" action="ExportMissionKML">
<p>Choose the time interval for events to export. You can specify either specific start and
end times, or one endpoint and duration for the time interval. You can also specify a
duration only, which implies you want the last X hours.
</p>

<table id="queryFields">
	<tr>
		<td><label for="min">Start Time:</label></td>
		<td><input id="min" type="text" name="startTime" /> (CoT format, e.g.:   2014-09-04T13:02:24.0Z   )</td>
	</tr>
	<tr>
		<td><label for="max">End Time:</label></td>
		<td><input id="max" type="text" name="endTime" /> (CoT format: e.g.:   2014-09-04T15:00:00.0Z   )</td>
	</tr>

	<tr>
		<td><label for="interval">Interval:</label></td> 
		<td><input id="interval" type="text" name="interval" /> hours (decimal)</td>
	</tr>

	<tr>
		<td><label for="uid">UID Filter:</label></td>
		<td><input id="uid" type="text" name="uid" /> </td>
	</tr>

	<tr>
		<td><label for="multiTrackThreshold">MultiTrack Threshold:</label></td>
		<td><input id="multiTrackThreshold" type="text" name="multiTrackThreshold" value="" /> minutes; NOTE: setting this to non-zero can make it harder for Google Earth to interpret</td>
	</tr>

	<tr>
		<td><label for="extendedData">Return Extended Data (course, speed, ce, le):</label></td>
		<td><select name="extendedData" id="extendedData"><option value="true">Yes</option><option value="false">No</option></select></td>
	</tr>

	<tr>
		<td><label for="optimizeExport">Remove Identical Position Reports:</label></td>
		<td><select name="optimizeExport" id="optimizeExport"><option value="true">Yes</option><option value="false">No</option></select> </td>
	</tr>
	
	<%
	if (request.getParameter("userGroupsOnly") == null) {
    %>
	
	<tr>
		<td>Group Filter:</td><td>
	<%
		GroupManager groupManager = (GroupManager) SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class);
		
		Set<String> groupNames = new HashSet<String>();
		
		for (Group group : groupManager.getAllGroups()) {
		    groupNames.add(group.getName());
		}
		
		for (String group : groupNames) {
	%><input type="checkbox" id="<%=ESAPI.encoder().encodeForHTML(group)%>" name="groups" value="<%=ESAPI.encoder().encodeForHTML(group)%>"><label><%=ESAPI.encoder().encodeForHTML(group)%></label>&nbsp;<%
		}
	%>
	</td></tr>
	<%
		}
	%>
</table>

<br/>

<table>
<tr><td>
<input type="radio" name="format" value="kml" />KML<br />
<input type="radio" name="format" value="kmz" checked />KMZ (stand-alone with icons embedded)<br /></td>
</tr>
</table>
<br />
 <p><input type="submit" value="Export KML/KMZ" /></p>

</form>

 <p><input id="copyKmlButton" type="button" value="Copy KML link" />  (use this if you're having trouble getting Google Earth to ask for authentication)</p>


<hr />
<%
	try {
%>
<p>Table of #CoT events by hour.  You can copy and paste the timestamps from this table into the fields above.</p>
<table border="1">
<thead>
	<tr>
		<td>Time</td>
		<td>Number of Events</td>
	</tr>
</thead>

<%
	final SimpleDateFormat cotDateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
	cotDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

	CommonUtil martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);
	KMLDao dao = (KMLDao) SpringContextBeanForApi.getSpringContext().getBean("kmlDao");
	
	String groupVector = martiUtil.getGroupBitVector(request);
	
	List<Association<Date, Long>> cotCounts;
	
    if (request.getParameter("userGroupsOnly") != null) {
        cotCounts = dao.getCotEventCountByHour(groupVector, false, true);
    } else {
        cotCounts = dao.getCotEventCountByHour(groupVector, true, false);
    }
	
	for(Association<Date, Long> count : cotCounts) {
%>	<tr>
		<td><%=ESAPI.encoder().encodeForHTML(cotDateFormat.format(count.getKey()))%></td>
		<td align="right"><%=ESAPI.encoder().encodeForHTML(Long.toString(count.getValue()))%></td>
	</tr>
<%
	}
%>

</table>
<%
	} catch (Exception e){
%>
        <h3> <Font COLOR="DC143C"> Error connecting to PostGreSQL Database: </Font> </h3>
        <p> Make sure that the database server is running, then refresh this page </p>
<%
	}
%>
<br />
<hr />

<%@ include file="footer.jsp" %>
	
</body>
</html>
