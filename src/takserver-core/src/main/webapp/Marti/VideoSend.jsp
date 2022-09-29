<%@ page import="java.rmi.Naming" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.ArrayList" %>        
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="com.bbn.marti.video.Feed" %>
<%@ page import="com.bbn.marti.video.Feed.Type" %>
<%@ page import="com.bbn.marti.video.VideoConnections" %>
<%@ page import="com.bbn.marti.remote.SubscriptionManagerLite" %>
<%@ page import="com.bbn.marti.remote.RemoteSubscription" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.bbn.marti.util.spring.SpringContextBeanForApi" %>
<%@ page import="com.bbn.marti.util.CommonUtil" %>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
   <title>Send Video Feed</title>
   <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
   <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
   <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
   <script type="text/javascript" src="jquery/jquery-ui.js"></script> 
   <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>

   <script language="javascript" type="text/javascript">
   
	$(document).ready(function() {
		
		$("#send").click(function(event) {
			var formData = $('#videoStreamForm').serialize();
			//alert(formData);
			$.ajax({
				url  : '/Marti/vcs?feedId=<%=ESAPI.encoder().encodeForURL(request.getParameter("feedId"))%>',
			   	type : 'POST',
			   	data : formData,
			   	contentType : 'application/x-www-form-urlencoded',
			})
           .done(function() {
           		window.location = "/Marti/VideoManager.jsp";
            })
           .fail(function() {
				$.jnotify("Failed to send video", "error");
            });
      	});
        
   	});
	
   </script>
   
</head>
<body>
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

<h1>Send Video Feed</h1>

<table border=1>
<tr>
  <th>Type</th>
  <th>Alias</th>
  <th>Protocol</th>
  <th>Address</th>
  <th>Port</th>
  <th>Path</th>
  <th>Preferred MAC address</th>
  <th>Rover Port</th>
  <th>Ignore Embedded KLV</th>
  <th>Network timeout</th>
  <th>Buffer time</th>
  <th>RTSP reliable</th>
  <th>Latitude</th>
  <th>Longitude</th>
  <th>Fov</th>
  <th>Heading</th>
  <th>Range</th>
</tr>

<%
	CommonUtil martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);
	String groupVector = martiUtil.getGroupBitVector(request);

	String[] feedIds = request.getParameter("feedId").split("\\|");
	for (String feedId : feedIds) {
		Feed feed = com.bbn.marti.util.spring.SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.video.VideoManagerService.class).getFeed(Integer.parseInt(feedId), groupVector);
%>
		<tr>
			<td><%=feed.getType().toString()%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getAlias())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getProtocol())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getAddress())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getPort())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getPath())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getMacAddress())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getRoverPort())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getIgnoreEmbeddedKLV())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getNetworkTimeout())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getBufferTime())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getRtspReliable())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getLatitude())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getLongitude())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getFov())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getHeading())%></td>
			<td><%=ESAPI.encoder().encodeForHTML(feed.getRange())%></td>
		</tr>
<%
	}
%>
</table>


<h2>Send To:</h2>

<form id="videoStreamForm">

<%
	SubscriptionManagerLite subscrMgr = SpringContextBeanForApi.getSpringContext().getBean(SubscriptionManagerLite.class);
	List<RemoteSubscription> subscriptions = subscrMgr.getSubscriptionList();
	List<RemoteSubscription> filtered = new ArrayList<RemoteSubscription>(subscriptions.size());

	// flatmap subscriptions list on basis of callsign being nonnull and nonempty
	for (RemoteSubscription subscr : subscriptions) {
	    if (subscr.callsign != null && !subscr.callsign.isEmpty()) {
	        // should never really be null, empty if unassigned
	        filtered.add(subscr);
	    }
	}

	// sort filtered by case-insensitive alphabetical
	// sortByCallsign comparator requires nonnull callsign
	Collections.sort(filtered, RemoteSubscription.sortByCallsign(false));

	for (RemoteSubscription subscr : filtered) {
%>
    	<input type="checkbox" name="contacts" value="<%=subscr.clientUid%>"/> <%=subscr.callsign%> 
    	<br/>
    <%
    	}
    %>

<br>
<input type="button" id="send" value="Send" />
<br>
<br>
</form>

<a href="VideoManager.jsp">Video Feed Manager</a>
<br><br>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
