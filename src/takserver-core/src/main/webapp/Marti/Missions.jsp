<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="com.bbn.marti.sync.model.Mission" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.bbn.security.web.SecurityUtils" %>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="com.bbn.marti.logging.AuditLogUtil" %>
<%@ page import="java.security.Principal" %>
<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <title>Mission Manager</title>
    <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
    <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
    <link rel="stylesheet" href="css/bootstrap-theme.min.css" />
    <link rel="stylesheet" href="css/bootstrap.min.css">
    <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
    <script type="text/javascript" src="jquery/jquery-ui.js"></script>
    <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
    <script type="text/javascript" src="Missions.js"></script>
</head>
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

<script language="javascript" type="text/javascript">

    function deleteMission(name) {

        if (!confirm("Press Ok to delete mission " + name)) {
            return;
        }

        $.ajax({
            url  : "api/missions/" + name + "?creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
            type : "DELETE",
            async : false,
            cache : false,
            contentType : false,
            processData : false,
            success: function (response){
                location.reload();
            },
            error : function(stat, err) {
                if (stat.responseJSON !== null && stat.responseJSON.message != ""){
                    $.jnotify("Error: " + stat.responseJSON.message, "error", 3000); 
                } else{
                    $.jnotify("Error deleting", "error", 3000);
                }
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

    function deleteSelected() {
        var selected = getSelected();
        if (selected.length == 0) {
            return;
        }

        for (var i=0; i<selected.length; i++) {
            deleteMission(selected[i]);
        }
    }
	
	function createMissionTable(missions) {
		var tbl = document.getElementById('missionTable');
		var rowNumber = 0;
		for (var i = 0; i < missions.length; i++) {
			var row = tbl.insertRow(rowNumber + 1);
			rowNumber++;

			var cell = 0;

			var expiration_string;
			if (missions[i].expiration != undefined && missions[i].expiration >= 0) {
			    expiration_string = new Date(missions[i].expiration * 1000).toISOString();
			} else {
			    expiration_string = "";
			}

			addCell(row, cell++, createDiv("<a href=\"api/missions/" +  encodeURI(missions[i].name) + "\">" + sanitize(missions[i].name) + "</a>"), 'top');
			addCell(row, cell++, document.createTextNode(missions[i].description), 'top');
			addCell(row, cell++, createDiv(createContents(missions[i].contents)), 'top');
			addCell(row, cell++, createDiv(createUids(missions[i].uids)), 'top');
			addCell(row, cell++, createDiv(createGroups(missions[i].groups)), 'top');
			addCell(row, cell++, createDiv(createKeywords(missions[i].keywords)), 'top');
			addCell(row, cell++, document.createTextNode(missions[i].creatorUid), 'top');
			addCell(row, cell++, document.createTextNode(missions[i].createTime), 'top');
			addCell(row, cell++, document.createTextNode(expiration_string), 'top');

            var controls =
                "<input type=\"button\" id=\"edit\" value=\"Edit\" onClick=\"window.location.href='MissionEditor.jsp?name=" + encodeURI(missions[i].name) + "';\"/>&nbsp;" +
                "<input type=\"button\" id=\"invite\" value=\"Invite\" onClick=\"window.location.href='MissionInvite.jsp?name=" + encodeURI(missions[i].name) + "';\"/>&nbsp;" +
                "<input type=\"button\" id=\"download\" value=\"Download\" onClick=\"window.location.href='/Marti/api/missions/" + encodeURI(missions[i].name) + "/archive';\"/>&nbsp;" +
                "<input type=\"button\" id=\"send\" value=\"Send\" onClick=\"window.location.href='MissionSend.jsp?name=" + encodeURI(missions[i].name) + "';\"/>&nbsp;" +
                "<input type=\"button\" id=\"delete\" value=\"Delete\" onClick=\"deleteMission('" + encodeURI(missions[i].name) + "');\"/>&nbsp;" +
                "<input type=\"button\" id=\"downloadKml\" value=\"Download KML\" onClick=\"window.location.href='/Marti/api/missions/" + encodeURI(missions[i].name) + "/kml?download=true';\"/>&nbsp;" +
                "<a href=\"api/missions/" +  encodeURI(missions[i].name) + "/kml\">kml network link</a>";
            addCell(row, cell++, createDiv(controls), 'top');
		}
	}

    function createArchivedMissions(missions) {
        var archiveDiv = document.getElementById('archivedMissions');
        if (missions.length == 0) {
            archiveDiv.appendChild(document.createTextNode("No archived missions found."));
        } else {
            for (var i = 0; i < missions.length; i++) {
                var anchor  = document.createElement("a");
                var text =  missions[i].name + " - " + missions[i].submissionTime;
                anchor.title = text;
                anchor.href = "/Marti/sync/content?hash=" + sanitize(missions[i].hash);
                anchor.appendChild(document.createTextNode(text));
                archiveDiv.appendChild(anchor);

                archiveDiv.appendChild(document.createElement("br"));
            }
        }
    }

	function getArchivedMissions() {
		$.ajax({
			url  : "api/sync/search?keyword=ARCHIVED_MISSION",
			type : "GET",
			async : false,
			cache : false,
			contentType : false,
			processData : false,
			success: function (response) {
				createArchivedMissions(response.data)
			},
			error : function(stat, err) {
				$.jnotify("Error getting missions", "error");
			}
		});
	}	
	
	function getMissions() {
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
				createMissionTable(response.data);
				getArchivedMissions();
				document.getElementById('loadingDiv').style.display = 'none';
			},
			error : function(stat, err) {
				$.jnotify("Error getting missions", "error");
			}
		});
	}	
</script>

<body onload="getMissions();">

<h1>Mission Manager</h1>

<table border=0 width=100%>
    <tr>
        <td align=left>
            <input type="button" id="add" value="Add New Mission" onClick="window.location.href='MissionEditor.jsp';"/>&nbsp;
        </td>
    </tr>
</table>

<br><br>
<form id="missionForm">
    <table id="missionTable" border=1 cellpadding=5 cellspacing=1>
        <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Contents</th>
            <th>UIDs</th>
            <th>Groups</th>
            <th>Keywords</th>
            <th>Creator Uid</th>
            <th>Create Time</th>
             <th>Expiration</th>
            <th>Actions</th>
        </tr>
    </table>
</form>


<br>
<h2>Deleted Missions</h2>
<div id="archivedMissions"></div>
<br>

<div id="loadingDiv"><p>Loading...</p></div>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
