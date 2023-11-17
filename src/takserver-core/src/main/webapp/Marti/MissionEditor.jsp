<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.bbn.marti.sync.model.MissionRole" %>
<%@ page import="com.bbn.marti.util.CommonUtil" %>
<%@ page import="com.bbn.marti.util.spring.SpringContextBeanForApi" %>

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
    <title>Mission Editor</title>
    <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
    <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
    <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
    <script type="text/javascript" src="jquery/jquery-ui.js"></script>
    <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
    <script type="text/javascript" src="Missions.js"></script>
</head>

<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

<script language="javascript" type="text/javascript">

    <% CommonUtil martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class); %>
    var isAdmin = <%= martiUtil.isAdmin() %>

    $(document).ready(function() {

            $("#addEditButton").click(function(event) {
                var mission = $('#missionForm').serializeArray();
                var name = mission[0].value;
                var url  = 'api/missions/' + name + '?creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>';

                var description = mission[1].value;
                if (description != null && description.length != 0) {
                    url += '&description=' + description;
                }

                var chatRoom = mission[2].value;
                if (chatRoom != null && chatRoom.length != 0) {
                    url += '&chatRoom=' + chatRoom;
                }

                var password = mission[3].value;
                if (password != null && password.length != 0) {
                    url += "&password=" + password;
                }

                if ($("#defaultRole").val() != null && $("#defaultRole").val() !== undefined) {
                    var defaultRole = $("#defaultRole").val();
                    if (defaultRole != null && defaultRole.length != 0) {
                        url += "&defaultRole=" + defaultRole;
                    }
                }

                if ($("#groups").val() != null && $("#groups").val() !== undefined) {
                    for (var i=0; i < $("#groups").val().length; i++) {
                        url += "&group=" + $("#groups").val()[i];
                    }
                }

                url += "&allowGroupChange=true";

                $.ajax({
                    url  : url,
                    type : 'PUT',
                })
                    .done(function() {
                        window.location.href="MissionEditor.jsp?name=" + name;
                    })
                    .fail(function() {
                        $.jnotify("Invalid Mission", "error");
                    });
            });

        });

        var _mission = null;

        function deleteContents(mission, hash) {
            if (!confirm("Press Ok to remove file from mission")) {
                return;
            }

            $.ajax({
                url  : "api/missions/" + mission + "/contents?hash=" + hash + "&creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "DELETE",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    window.location.href="MissionEditor.jsp?name=" + mission;
                },
                error : function(stat, err) {
                    $.jnotify("Error deleting contents", "error");
                }
            });
        }

        function setPassword() {
            if (!confirm("Press Ok to set new password")) {
                return;
            }

            $.ajax({
                url  : "api/missions/" + _mission.name  + "/password?password=" + document.getElementById('newPassword').value + "&creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "PUT",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    window.location.href="MissionEditor.jsp?name=" + _mission.name ;
                },
                error : function(stat, err) {
                    $.jnotify("Error setting password", "error");
                }
            });
        }

        function removePassword() {
            if (!confirm("Press Ok to remove password protection")) {
                return;
            }

            $.ajax({
                url  : "api/missions/" + _mission.name + "/password?creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "DELETE",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    window.location.href="MissionEditor.jsp?name=" + _mission.name ;
                },
                error : function(stat, err) {
                    $.jnotify("Error removing password", "error");
                }
            });
        }

        function addPasswordToEditor() {

            // hide password fields, only used for mission creation
            document.getElementById('passwordRow').style.display = 'none';

            var html = "<br><h3>Password</h3><table border=0>";

            html += "<tr><td>This mission is";
            if (!_mission.passwordProtected) html += " not";
            html += " password protected</td></tr>";

            html += "<tr><td>" +
                "<input type=\"password\" style=\"width:415px\" id=\"newPassword\" " +
                "onkeyup='document.getElementById(\"setPasswordButton\").disabled = document.getElementById(\"newPassword\").value.length == 0'  />" +
                "&nbsp;&nbsp;<input type=\"button\" id=\"setPasswordButton\" value=\"Set\" onClick=\"setPassword()\" style=\"width:50px\" disabled />";

            if (_mission.passwordProtected) {
                html += "&nbsp;&nbsp;<input type=\"button\" id=\"removePasswordButton\" value=\"Remove\" onClick=\"removePassword()\" />";
            }

            html += "</tr></td>";
            html += "</table>"
            var passwordDiv = document.getElementById('passwordDiv');
            passwordDiv.innerHTML = html;
        }

        function addDefaultRoleToEditor(defaultRole) {

            if (!isAdmin) {
                document.getElementById('defaultRoleRow').style.display = 'none';
                return;
            };

            var html = "<select name=\"defaultRole\" id=\"defaultRole\" style=\"width:250px\" >";
            <% for (MissionRole.Role role : MissionRole.Role.values()) { %>
            html += "<option ";

            if (defaultRole == null || defaultRole.type == null) {
                if (<%=role == MissionRole.defaultRole%>) {
                    html += "selected"
                }
            } else if (defaultRole.type === "<%=role.name()%>") {
                html += "selected"
            }

            html += "><%=role.name()%></option>";
            <% } %>
            html += "</select>";

            var defaultRoleDiv = document.getElementById('defaultRoleDiv');
            defaultRoleDiv.innerHTML = html;
        }

        function addGroupsToEditor(allGroups, missionGroups) {

            var html = "<select name=\"groups\" id=\"groups\" multiple style=\"width:250px\" >";
            for (var i = 0; i < allGroups.length; i++) {
                html += "<option ";
                if (missionGroups != null && missionGroups.includes(allGroups[i].name)) {
                    html += "selected";
                }
                html += ">" + allGroups[i].name + "</option>";
            }
            html += "</select>";

            var groupsDiv = document.getElementById('groupsDiv');
            groupsDiv.innerHTML = html;
        }

        function loadGroups(missionGroups) {
            $.ajax({
                url  : "api/groups/all",
                type : "GET",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    addGroupsToEditor(response.data, missionGroups);
                },
                error : function(stat, err) {
                    $.jnotify("Error getting mission", "error");
                }
            });
        }


        function addFeed() {
            if ($("#feeds").val() != null && $("#feeds").val() !== undefined) {
                $.ajax({
                    url  : "api/missions/" + _mission.name + "/feed?dataFeedUid=" + $("#feeds").val() + "&creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                    type : "POST",
                    async : false,
                    cache : false,
                    processData: false,
                    contentType: 'application/json',
                    dataType: 'text',
                    success: function (response) {
                        window.location.reload();
                    },
                    error : function(stat, err) {
                        if (stat >= 200 || stat < 300) {
                            window.location.reload();
                        }
                        $.jnotify("Error adding feed to mission", "error");
                    }
                });
            }
        }

        function deleteFeed(missionFeedUid) {
            if (!confirm("Press Ok to remove Feed from mission")) {
                return;
            }
            $.ajax({
                url  : "api/missions/" + _mission.name + "/feed/" + missionFeedUid + "?creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "DELETE",
                async : false,
                cache : false,
                contentType: 'application/json',
                dataType: 'text',
                processData: false,
                success: function (response) {
                    window.location.reload();
                },
                error : function(stat, err) {
                    if (stat >= 200 || stat < 300) {
                        window.location.reload();
                    }
                    $.jnotify("Error removing feed from mission", "error");
                }
            });
        }

        function addFeedsToEditor(allFeeds, missionFeeds) {
            var html = "<br><h3>Feeds</h3><table>";
            if (missionFeeds.length > 0) {
                for (var i = 0; i < missionFeeds.length; i++) {
                    html += "<tr>"
                    html += "<td><a href=\"inputs/index.html#!/modifyPluginDataFeed/" + missionFeeds[i].name + "\">";
                    html += missionFeeds[i].name + "</a>&nbsp;";
                    html += "<input type=button id=\"Delete\" value=\"Remove\" onClick=\"deleteFeed('" + missionFeeds[i].uid + "');\"></td>"
                    html += "</tr>"
                }
            } else {
                html += "<tr><td>No Feeds have been added</td></tr>"
            }
            html += "<tr><td><select name=\"feeds\" id=\"feeds\" style=\"width:415px\">";
            for (var i = 0; i < allFeeds.length; i++) {
                if (missionFeeds == null || !missionFeeds.some(feed => feed.dataFeedUid == allFeeds[i].uuid)) {
                    html += "<option value=\"" + allFeeds[i].uuid + "\">" + allFeeds[i].name + "</option>";
                }
            }
            html += "</select>&nbsp;&nbsp;";
            html += "<input type = \"button\" id=\"feedButton\" onclick = \"addFeed();\" value = \"Add\" style=\"width:50px\"></td></tr>"
            html += "</table>";

            var feedsDiv = document.getElementById('feedsDiv');
            feedsDiv.innerHTML = html;
        }

        function loadFeeds(missionFeeds) {
            $.ajax({
                url  : "api/datafeeds",
                type : "GET",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    addFeedsToEditor(response.data, missionFeeds);
                },
                error : function(stat, err) {
                    $.jnotify("Error getting mission feeds", "error");
                }
            });
        }

        function setExpirationNull() {
            console.log("setting expiration value to null");
            var expiration = document.getElementById('expirationValue');
            expiration.value = null;
        }

        function setExpiration() {
            var expiration = $("#expirationValue").val();
            console.log(expiration);
            var retVal;
            if (expiration === null || expiration === undefined || expiration === '') {
                console.log("a null value for expiration?");
                setExpirationNull();
                retVal = -1;
            } else {
                var newDate = new Date(expiration);
                retVal = (newDate.getTime() - (60000 * newDate.getTimezoneOffset())) / 1000;
            }
            var mission_name = $('#name').val();
            var url  = 'api/missions/' + mission_name + "/expiration";
            url += "?expiration=" + retVal;
            console.log(url);

            $.ajax({
                url  : url,
                type : 'PUT',
                success: function (response) {
                    $.jnotify("expiration set successfully")
                },
                error : function(stat, err) {
                    $.jnotify("Error setting expiration", "error");
                }
            })
        }

        function addExpirationToEditor(expiration) {
            console.log(expiration);
            var date_string;
            if (_mission.expiration < 0 || _mission.expiration === undefined || _mission.expiration === null || _mission.expiration === "") {
                var date_string = "";
            } else {
                var date_string = new Date(expiration * 1000).toISOString().slice(0, 19);
            }
            var html = "<br><h3>Expiration</h3><table border=0>";
            html += "<tr>";
            html += "<td><input type=\"datetime-local\" id=\"expirationValue\" value=" + date_string + "></td>";
            html += "<td><input type=\"button\" onClick=\"setExpirationNull()\" value=\"Clear\"></td>";
            html += "</tr>";
            html += "<tr><td><input type=\"button\" onClick=\"setExpiration()\" value=\"Set\"></td></tr></table>"
            var expirationDiv = document.getElementById("expirationDiv");
            expirationDiv.innerHTML = html;
        }

        function addContentsToEditor(contents) {
            var html = "<br><h3>Contents</h3><table border=0>";
            if (contents.length > 0) {
                for (var i = 0; i < contents.length; i++) {
                    html += "<tr>"
                    html += "<td><a href=\"sync/content?hash=" + contents[i].data.hash + "\">" + contents[i].data.name + "</a></td>";
                    html += "<td><input type=button id=\"Delete\" value=\"Remove\" onClick=\"deleteContents('" + _mission.name + "','" + contents[i].data.hash + "');\"></td>"
                    html += "</tr>"
                }
            } else {
                html += "<tr><td>No files have been added</td></tr>"
            }

            html += "<tr><td><br><input type=\"button\" id=\"addFile\" value=\"Add File\" onClick=\"window.location.href='upload.jsp?name=" + _mission.name + "';\"/></td></tr>"
            html += "</table></td></tr></table>"
            var contentsDiv = document.getElementById('contentsDiv');
            contentsDiv.innerHTML = html;
        }

        function addKeywords() {
            var keywords = $('#keywords').val().trim();

            $.ajax({
                url  : "api/missions/" + _mission.name + "/keywords",
                type : "PUT",
                data : JSON.stringify(keywords.split(' ')),
                async : false,
                cache : false,
                contentType : "application/json",
                processData : false,
                success: function (response) {
                    $.jnotify("Keywords set successfully")
                },
                error : function(stat, err) {
                    $.jnotify("Error adding keywords", "error");
                }
            });
        }

        function addKeywordsToEditor(keywords) {
            var html = "<br><h3>Keywords</h3>";
            var keywordText = "";
            if (keywords.length > 0) {
                for (var i=0; i < keywords.length; i++) {
                    keywordText += keywords[i] + ' ';
                }
            }
            html += "<table><tr><td><input type=\"text\" style=\"width:415px\" id=\"keywords\" value=\""
                + keywordText
                + "\">&nbsp;&nbsp;<input type=\"button\" id=\"keywordsButton\" value=\"Set\" onClick=\"addKeywords()\" style=\"width:50px\" /></td></tr></table>";


            var keywordsDiv = document.getElementById('keywordsDiv');
            keywordsDiv.innerHTML = html;
        }

        function addUid() {
            var uid = document.getElementById('uid').value;
            var json = "{ \"uids\":[ \"" + uid+ "\" ] }";
            $.ajax({
                url  : "api/missions/" + _mission.name + "/contents?creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "PUT",
                data : json,
                async : false,
                cache : false,
                contentType : "application/json",
                processData : false,
                success: function (response) {
                    window.location.reload();
                },
                error : function(stat, err) {
                    $.jnotify("Error adding uid to mission", "error");
                }
            });
        }

        function deleteUid(uid) {
            if (!confirm("Press Ok to remove UID from mission")) {
                return;
            }

            $.ajax({
                url  : "api/missions/" + _mission.name + "/contents?uid=" + uid + "&creatorUid=<%=URLEncoder.encode(AuditLogUtil.getUsername(), "UTF-8")%>",
                type : "DELETE",
                async : false,
                cache : false,
                processData : false,
                success: function (response) {
                    window.location.reload();
                },
                error : function(stat, err) {
                    $.jnotify("Error removing uid from mission", "error");
                }
            });
        }

        function addUidsToEditor(uids) {
            var html = "<br><h3>UIDs</h3><table>";
            if (uids.length > 0) {
                for (var i = 0; i < uids.length; i++) {
                    html += "<tr>"
                    html += "<td><a href=\"api/cot/xml/" + uids[i].data + "\">" + uids[i].data + "</a>&nbsp;";
                    html += "<input type=button id=\"Delete\" value=\"Remove\" onClick=\"deleteUid('" + uids[i].data + "');\"></td>"
                    html += "</tr>"
                }
            } else {
                html += "<tr><td>No UIDs have been added</td></tr>"
            }

            html += "<tr><td><input type=\"text\" style=\"width:415px\" id=\"uid\">&nbsp;&nbsp;<input type=\"button\" id=\"uidButton\" value=\"Add\" onClick=\"addUid();\" style=\"width:50px\"/></td></tr>";
            html += "</table>";

            var uidsDiv = document.getElementById('uidsDiv');
            uidsDiv.innerHTML = html;
        }

        function addMissionToEditor() {
            $("#name").val(_mission.name);
            $("#description").val(_mission.description);
            $("#chatRoom").val(_mission.chatRoom);
            $("#expirationValue").val(_mission.expiration);

            $("#addEditButton").val("Save");

            addExpirationToEditor(_mission.expiration);
            addContentsToEditor(_mission.contents);
            addUidsToEditor(_mission.uids);
            addKeywordsToEditor(_mission.keywords);
            addPasswordToEditor();
            addDefaultRoleToEditor(_mission.defaultRole);
            loadFeeds(_mission.feeds);
            loadGroups(_mission.groups);
        }

        function loadMission() {

            var mission = $.urlParam('name');
            if (mission == null) {
                return;
            }

            $("#title").html("Edit Mission");

            $.ajax({
                url  : "api/missions/" + mission,
                type : "GET",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    if (response.data != null && response.data.length==1) {
                        _mission = response.data[0];
                    }
                    
                    console.log("got mission: " + _mission);

                    addMissionToEditor();
                },
                error : function(stat, err) {
                    $.jnotify("Error getting mission", "error");
                }
            });
        }

</script>

<body onLoad="addDefaultRoleToEditor(null);loadGroups(null);loadMission();">


<h1><div id=title>Add Mission</div></h1>
<form id="missionForm">
    <table>
        <tr><td width=150 valign=top>Name</td><td><input type="text" name="name" id="name" style="width:250px" /></td></tr>
        <tr><td width=150 valign=top>Description</td><td><input type="text" name="description" id="description" style="width:250px" /></td></tr>
        <tr><td width=150 valign=top>Chat Room</td><td><input type="text" name="chatRoom" id="chatRoom" style="width:250px" /></td></tr>
        <tr id="passwordRow"><td width=150 valign=top>Password</td><td><input type="password" name="password" id="password" style="width:250px" /></td></tr>
        <tr id="defaultRoleRow"><td width=150 valign=top>Default Role</td><td><div id="defaultRoleDiv"></div></td></tr>
        <tr id="groupsRow"><td width=150 valign=top>Groups</td><td><div id="groupsDiv"></div></td></tr>
    </table>
    <br>
    <input type="button" id="addEditButton" value="Add" />
    <br>

    <div id="expirationDiv"></div>
    <div id="contentsDiv"></div>
    <div id="uidsDiv"></div>
    <div id="feedsDiv"></div>
    <div id="keywordsDiv"></div>
    <div id="passwordDiv"></div>

</form>

<br>
<br>
<a href="Missions.jsp">Mission Manager</a>
<br><br>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
