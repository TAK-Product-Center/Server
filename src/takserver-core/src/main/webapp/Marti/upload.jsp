<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
   <title>File Uploading Form</title>
   <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
   <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
   <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
   <script type="text/javascript" src="jquery/jquery-ui.js"></script> 
   <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
</head>
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>


<script language="javascript" type="text/javascript">

	var _missions = null;

	$.urlParam = function(name){
		var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
		if (results==null){
		   return null;
		}
		else{
		   return decodeURI(results[1]) || 0;
		}
	}
	
    function addMissionsToSelect(missions) {
        var missionDiv = document.getElementById('missionDiv');
        for (var i = 0; i < missions.length; i++) {
            var input  = document.createElement("input");
            input.setAttribute("type", "checkbox");
            input.setAttribute("id", missions[i].name);
            if ($.urlParam('name') == missions[i].name) {
                input.setAttribute("checked", "checked");
            }
            missionDiv.appendChild(input);
            missionDiv.appendChild(document.createTextNode(missions[i].name));
            missionDiv.appendChild(document.createElement("br"));
        }
    }

	function loadMissions() {
        $.ajax({
            url  : "api/missions",
            type : "GET",
            data: { 
                passwordProtected: "true", 
                defaultRole: "true"
            },
            async : false,
            cache : false,
            contentType : false,
            processData : true,
            success: function (response) {
				_missions = response.data;
				addMissionsToSelect(response.data);
            },
            error : function(stat, err) {
                $.jnotify("Error getting missions", "error");
            }
        });
	}

	function addMissionContents(hash, mission) {
		var json = "{ \"hashes\":[ \"" + hash + "\" ] }";
        $.ajax({
            url  : "api/missions/" + mission + "/contents?creatorUid=<%=ESAPI.encoder().encodeForURL(AuditLogUtil.getUsername())%>",
            type : "PUT",
			data : json,
            async : false,
            cache : false,
            contentType : "application/json",
            processData : false,
            success: function (response) {
            },
            error : function(stat, err) {
                $.jnotify("Error adding file to mission", "error");
            }
        });
	}
	
   $(document).ready(function() {
      $("form[name=uploadForm]").submit(function(event) {
         event.preventDefault();
         var formData = new FormData($(this)[0]);
          $.jnotify("Uploading...", 0);
          $.jnotify.pause();
          $.ajax({
            url  : "sync/upload",
            type : "POST",
            data : formData,
            async : true,
            cache : false,
            contentType : false,
            processData : false,
            success : function(returnData) {
                $.jnotify.resume();

				for (var i = 0; i < _missions.length; i++) {
					if ($("[id='" + _missions[i].name + "']")[0].checked) {
						addMissionContents(JSON.parse(returnData).Hash, _missions[i].name);
					}
				}
				$.jnotify("Successfully uploaded file");
				
				if ($.urlParam('name') != null) {
				    window.history.back();
				}
            },
            error : function(stat, err) {
               $.jnotify.resume();
               $.jnotify("Error submitting file", "error");
            }
         });
         return false;
      });
   });
</script>

<body onload="loadMissions();">
<h1>File Upload:</h1>

Select a file to upload: <br />
<form name="uploadForm">
<input type="file" name="assetfile" size="50" />
<br />
<p>Resource Name (optional):</p>
<input type="text" name="Name" size="100" />
<br />
<p>Download Path (optional):</p>
<input type="text" name="DownloadPath" size="100" />
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
<h2>Mission</h2>
<p>You may optionally associate the file with a mission.</p>
<div id="missionDiv" ></div>
<br />
<br />

<input type="submit" value="Upload File" />

</form>

<a href="EnterpriseSync.jsp">Back to Enterprise Sync</a>

<hr />
	<%@ include file="footer.jsp" %>
</body>
</html>
