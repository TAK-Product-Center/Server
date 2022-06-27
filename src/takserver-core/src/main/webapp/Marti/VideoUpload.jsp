<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.bbn.marti.video.Feed" %>


<%
	String title = "Add";
  String action = "Add";
  String active = "true";
  String uuid = UUID.randomUUID().toString();
  String alias = "";
  String protocol = "";
  String address = "";
  String port = "";
  String path = "";
  String preferredMacAddress = "";
  String roverPort = "";
  String ignoreEmbeddedKLV = "";
  String timeout = "5000";
  String buffer = "";
  String rtspReliable = "";
  String latitude = "";
  String longitude = "";
  String fov = "";
  String heading = "";
  String range = "";
  String classification = "";
  String thumbnail = "";

  String feedId = request.getParameter("feedId");
  if (feedId != null) {
    title = "Edit";
    action = "Save";
    Feed feed = com.bbn.marti.util.spring.SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.video.VideoManagerService.class).getFeed(Integer.parseInt(feedId));
    uuid = feed.getUuid();
    active = Boolean.toString(feed.getActive());
    alias = feed.getAlias();
    protocol = feed.getProtocol();
    address = feed.getAddress();
    port = feed.getPort();
    path = feed.getPath();
    preferredMacAddress = feed.getMacAddress();
    roverPort = feed.getRoverPort();
    ignoreEmbeddedKLV = feed.getIgnoreEmbeddedKLV();
    timeout = feed.getNetworkTimeout();
    buffer = feed.getBufferTime();
    rtspReliable = feed.getRtspReliable();
    latitude = feed.getLatitude();
    longitude = feed.getLongitude();
    fov = feed.getFov();
    heading = feed.getHeading();
    range = feed.getRange();
    classification = feed.getClassification();
    thumbnail = feed.getThumbnail();
  }
%>

<%!
	public String setValue(String value) {
		if (value == null) {
			return "";
		}
	
		return "value=\"" + ESAPI.encoder().encodeForHTML(value) + "\"";
	}

%>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <style>
    input, select {
      box-sizing: border-box;
      -moz-box-sizing: border-box;
      -webkit-box-sizing: border-box;
    }
  </style>
  <title><%=title%>  Video Feed</title>
   <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
   <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
   <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
   <script type="text/javascript" src="jquery/jquery-ui.js"></script> 
   <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>

   <script language="javascript" type="text/javascript">
   
	$(document).ready(function() {
		
		$("#add").click(function(event) {
			var formData = $('#videoStreamForm').serialize();
			//alert(formData);
			$.ajax({
				url  : '/Marti/vcu?feedId=<%=ESAPI.encoder().encodeForURL(request.getParameter("feedId"))%>',
			   	type : 'POST',
			   	data : formData,
			   	contentType : 'application/x-www-form-urlencoded',
			})
           .done(function() {
           		window.location = "/Marti/VideoManager.jsp";
            })
           .fail(function() {
				$.jnotify("Invalid video feed", "error");
            });
      	});
        
   	});
    
    function setSelects() {
      $("#protocol").val("<%=ESAPI.encoder().encodeForHTML(protocol)%>");
    }

   </script>
   
</head>
<body onLoad="setSelects();">
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>
<h1><%=title%> Video Feed</h1>
<form id="videoStreamForm">
<input type="hidden" name="uuid" id="uuid" value="<%=ESAPI.encoder().encodeForHTML(uuid)%>" />
<table>
<tr><td>Active</td><td>
  <input type="checkbox" name="active" id="active" <%if (active.equals("true")) {%>checked <%}%> />
</td></tr>
<tr><td>Alias</td><td><input type="text" name="alias" style="width:250px" <%=setValue(alias)%> /></td></tr>

<tr><td>Protocol</td><td>
  <select name="protocol" id="protocol" style="width:250px" >
    <option value="udp">udp</option>
    <option value="rtsp">rtsp</option>
    <option value="rtmp">rtmp</option>
    <option value="rtmps">rtmps</option>
    <option value="tcp">tcp</option>
    <option value="rtp">rtp</option>
    <option value="http">http</option>
    <option value="https">https</option>
    <option value="raw">raw</option>
  </select>
</td></tr>
<tr><td>Address</td><td><input type="text" name="address" style="width:250px" <%=setValue(address)%> /></td></tr>
<tr><td>Port</td><td><input type="text" name="port" style="width:250px" <%=setValue(port)%> /></td></tr>
<tr><td>Path</td><td><input type="text" name="path" style="width:250px" <%=setValue(path)%> /></td></tr>
<tr><td>Preferred MAC address</td><td><input type="text" name="preferredMacAddress" style="width:250px" <%=setValue(preferredMacAddress)%> /></td></tr>
<tr><td>Rover Port</td><td><input type="text" name="roverPort" style="width:250px" <%=setValue(roverPort)%> /></td></tr>
<tr><td>Ignore Embedded KLV</td><td>
  <input type="checkbox" name="ignoreEmbeddedKLV" id="ignoreEmbeddedKLV" <%if (ignoreEmbeddedKLV.equals("true")) {%>checked<%}%> >
</td></tr>
<tr><td>Network timeout (ms)</td><td><input type="text" name="timeout" style="width:250px" <%=setValue(timeout)%> /></td></tr>
<tr><td>Buffer time (ms)</td><td><input type="text" name="buffer" style="width:250px" <%=setValue(buffer)%> /></td></tr>
<tr><td>RTSP reliable</td><td>
  <input type="checkbox" name="rtspReliable" id="rtspReliable" <%if (rtspReliable.equals("1")) {%>checked<%}%> >
</td></tr>
<tr><td>Latitude (decimal degrees)</td><td><input type="text" name="latitude" style="width:250px" <%=setValue(latitude)%> /></td></tr>
<tr><td>Longitude (decimal degrees)</td><td><input type="text" name="longitude" style="width:250px" <%=setValue(longitude)%> /></td></tr>
<tr><td>Field of view</td><td><input type="text" name="fov" style="width:250px" <%=setValue(fov)%> /></td></tr>
<tr><td>Heading</td><td><input type="text" name="heading" style="width:250px" <%=setValue(heading)%> /></td></tr>
<tr><td>Range</td><td><input type="text" name="range" style="width:250px" <%=setValue(range)%> /></td></tr>
<tr><td>Thumbnail</td><td><input type="text" name="thumbnail" style="width:250px" <%=setValue(thumbnail)%> /></td></tr>
<tr><td>Classification</td><td><input type="text" name="classification" style="width:250px" <%=setValue(classification)%> /></td></tr>

</table>
<br>
<input type="button" id="add" value="<%=action%>" />
<br>
<br>
</form>

<a href="VideoManager.jsp">Video Feed Manager</a>
<br><br>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
