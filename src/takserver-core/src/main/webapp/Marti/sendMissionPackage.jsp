<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <link rel="icon" type="image/x-icon" href="favicon.ico" />
   <title>File Uploading Form</title>
		<%@ page import="java.rmi.Naming" %>
		<%@ page import="java.net.URI" %>
 		<%@ page import="java.util.List" %>
		<%@ page import="java.util.Map" %>
		<%@ page import="java.util.Set" %>
        <%@ page import="java.util.ArrayList" %>        
        <%@ page import="java.util.LinkedList" %>
		<%@ page import="java.util.Collection" %>
		<%@ page import="java.util.Collections" %>
		<%@ page import="com.bbn.marti.remote.SubscriptionManagerLite" %>
		<%@ page import="com.bbn.marti.remote.RemoteSubscription" %>
		<%@ page import="com.bbn.marti.util.spring.SpringContextBeanForApi" %>
		
		
		
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

<script language="javascript" type="text/javascript">
   $(document).ready(function() {
      $("#anotherFile").click(function(event) {
         var buttonsToAdd = '';
         for(i=0; i<10; i++) {
            buttonsToAdd += '<br /><input type="file" name="assetfile" size="50" multiple="true"/>';
         }
         $("#moreFiles").html(buttonsToAdd);
      });

      $("form[name=uploadForm]").submit(function(event) {
         event.preventDefault();
         $('input[type=submit]').prop('disabled', true);
         var formData = new FormData($('form[name=uploadForm]')[0]);
         $.ajax({
            url  : "sync/missioncreate",
            type : "POST",
            data : formData,
            async : true,
            cache : false,
            contentType : false,
            processData : false,
            success : function(returnData) {
               $.jnotify("Successfully uploaded file");
               $('input[type=submit]').prop('disabled', false);
            },
            error : function(stat, err) {
               $.jnotify("Error submitting file", "error");
               $('input[type=submit]').prop('disabled', false);
            }
         });
         return false;
      });
   });
</script>

<h1>Mission Package Upload</h1>
Select files to include in the mission package: <br />
<form name="uploadForm">
<input type="file" name="assetfile" size="50" multiple="true"/>
<div id="moreFiles"></div>
<button type="button" id="anotherFile">Add more files</button>
<br />
Filename: <input type="text" name="filename" size="100" />
<br />
<h2>Contacts</h2>
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
				            out.print("<input type=\"checkbox\" name=\"contacts\" value=\""+subscr.clientUid+"\"/>" + subscr.callsign + "<br/>");
				        }
		%>
<%--
<br />
<h2>Resource Location</h2>
<p>You may enter an optional location to associate with the uploaded resource.
For example, if the resource is a photo, the location could represent the
place where the photo was taken.</p>
<p>Location is optional but ATAK can only retrieve resources whose location
metadata is defined.</p>
<br />
<p>Latitude (decimal degrees): <input type="text" name="Latitude" size="100" /></p>
<p>Longitude (decimal degrees): <input type="text" name="Longitude" size="100" /></p>
<br />
--%>
<input type="submit" value="Send Mission Package" />

</form>
<%@ include file="footer.jsp" %>

</body>
</html>
