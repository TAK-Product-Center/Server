<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="com.bbn.marti.video.Feed" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="com.bbn.marti.remote.util.SpringContextBeanForApi" %>
<%@ page import="com.bbn.marti.video.VideoManagerService" %>
<%@ page import="com.bbn.marti.video.VideoConnections" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.bbn.marti.util.CommonUtil" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache"> 
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <title>Video Feed Manager</title>
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
	ApplicationContext springContext = SpringContextBeanForApi.getSpringContext();
	VideoManagerService videoService = springContext.getBean(VideoManagerService.class);
%>

<script language="javascript" type="text/javascript">

	function deleteVideo(id) {
	    $.ajax({
	        url  : "vcm?id=" + id,
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
	
	function sendSelected() {
		var selected = getSelected();
	    if (selected.length == 0) {
	      return;
	    }
    
		var queryString = "";
		for (var i=0; i<selected.length; i++) {
			queryString += selected[i];
      		if (i<selected.length-1) queryString += "|";
		}
		
		window.location = "VideoSend.jsp?feedId=" + queryString;		
	}
	
	function deleteSelected() {
		var selected = getSelected();
		if (selected.length == 0) {
			return;
		}
    
		if (!confirm("Press Ok to confirm deletion")) {
			return;
		}
    
		for (var i=0; i<selected.length; i++) {
			deleteVideo(selected[i]);
		}
    
    	location.reload();
	}
	
	function setActive(id, active) {
	    $.ajax({
	        url  : "vcm?action=setActive&id=" + id + "&active=" + active,
	        type : "POST",
	        async : false,
	        cache : false,
	        contentType : false,
	        processData : false,
	        error : function(stat, err) {
	           $.jnotify("Error setting active status", "error");
	        }
	    });
	}	
	
	function setSelectedActive(active) {
		var selected = getSelected();
		if (selected.length == 0) {
			return;
		}
        
		for (var i=0; i<selected.length; i++) {
			setActive(selected[i], active);
		}
    
    	location.reload();    	
	}		

	function editSelected(active) {
		var selected = getSelected();
		if (selected.length == 0) {
			return;
		}

		if (selected.length > 1) {
      alert('You can only edit 1 video at a time.');
			return;
		}

		window.location = "VideoUpload.jsp?feedId=" + selected[0];		
	}		

</script>

<h1>Video Feed Manager</h1>

<input type="button" id="add" value="Add" onClick="window.location.href='VideoUpload.jsp';"/>&nbsp;
<input type="button" id="edit" value="Edit" onClick="editSelected();"/>&nbsp;
<input type="button" id="send" value="Send" onClick="sendSelected();"/>&nbsp;
<input type="button" id="delete" value="Delete" onClick="deleteSelected();"/>&nbsp;
<input type="button" id="active" value="Set active" onClick="setSelectedActive(true);"/>&nbsp;
<input type="button" id="inactive" value="Set inactive" onClick="setSelectedActive(false);"/>&nbsp;
<input type="button" id="all" value="Select all" onClick="selectAll();"/>&nbsp;

<br><br>
<form id="videoStreamForm">

<table border=1>
<tr>
  <th>Select</th>
  <th>Active</th>
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
  <th>Type</th>
  <th>Thumbnail</th>
  <th>Classification</th>
</tr>

<%
    CommonUtil martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);
    String groupVector = martiUtil.getGroupBitVector(request);
	VideoConnections videoConnections = videoService.getVideoConnections(false, true, groupVector);
	for (Feed feed : videoConnections.getFeeds()) {
%>
    <tr>
			<td align=center><input type="checkbox" name="feeds" value="<%=feed.getId()%>" autocomplete="off"/></td>
			<td><%=feed.getActive()%></td>
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
			<td><%=feed.getType().toString()%></td>
            <td><%=ESAPI.encoder().encodeForHTML(feed.getThumbnail())%></td>
            <td><%=ESAPI.encoder().encodeForHTML(feed.getClassification())%></td>
      </tr>
<%
	}
%>
</table>
</form>

<hr />
	<%@ include file="footer.jsp" %>
</body>
</html>
