<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="com.bbn.marti.sync.model.Mission" %>
<%@ page import="com.bbn.marti.remote.SubscriptionManagerLite" %>
<%@ page import="com.bbn.marti.remote.RemoteSubscription" %>
<%@ page import="java.rmi.Naming" %>
<%@ page import="org.springframework.beans.factory.annotation.Autowired" %>
<%@ page import="com.bbn.marti.sync.repository.MissionRepository" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Mission Invite</title>
    <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
    <link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
    <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
    <script type="text/javascript" src="jquery/jquery-ui.js"></script>
    <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
    <script type="text/javascript" src="Missions.js"></script>

    <script language="javascript" type="text/javascript">

        function createMissionTable(missions) {
            var tbl = document.getElementById('missionTable');
            var row = tbl.insertRow(1);
            var cell = 0;

            addCell(row, cell++, createDiv("<a href=\"api/missions/" +  _mission.name + "\">" + _mission.name + "</a>"), 'top');
            addCell(row, cell++, document.createTextNode(_mission.description), 'top');
            addCell(row, cell++, createDiv(createContents(_mission.contents)), 'top');
            addCell(row, cell++, createDiv(createUids(_mission.uids)), 'top');
			addCell(row, cell++, createDiv(createGroups(_mission.groups)), 'top');
            addCell(row, cell++, createDiv(createKeywords(_mission.keywords)), 'top');
            addCell(row, cell++, document.createTextNode(_mission.creatorUid), 'top');
            addCell(row, cell++, document.createTextNode(_mission.createTime), 'top');
            addCell(row, cell++, createDiv('', _mission.name + '_subs' ), 'top');
        }

        function loadMission() {

            var mission = $.urlParam('name');
            if (mission == null) {
                return;
            }

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

                    createMissionTable(response.data);
                    loadMissionSubscriptions();
                },
                error : function(stat, err) {
                    $.jnotify("Error getting missions", "error");
                }
            });
        }

        function createMissionContactsList(contacts) {
            var missionContactsDiv = document.getElementById('missionContacts');

            var html = "";
            for (var i=0; i < contacts.length; i++) {
                html += "<input type=\"checkbox\" name=\"contacts\" value=\"" + contacts[i].uid + "\"/>" +  contacts[i].callsign + " (" + contacts[i].uid + ")<br>";
            }

            missionContactsDiv.innerHTML = html;
        }

        function loadMissionContacts() {

            var mission = $.urlParam('name');
            if (mission == null) {
                return;
            }

            $.ajax({
                url  : "api/contacts/all",
                type : "GET",
                data: { 
                    noFederates: "true"
                },
                async : false,
                cache : false,
                contentType : false,
                processData : true,
                success: function (response) {
                    createMissionContactsList(response);
                },
                error : function(stat, err) {
                    $.jnotify("Error getting mission contacts", "error");
                }
            });
        }

        function createMissionSubscriptionList(mission, subscriptions) {

            var missionSubscriptionsDiv = document.getElementById(mission + '_subs');

            var html = "";
            for (var i=0; i < subscriptions.length; i++) {
                html += subscriptions[i] + "<br>";
            }

            missionSubscriptionsDiv.innerHTML = html;
        }

        function loadMissionSubscriptions() {

            var mission = $.urlParam('name');
            if (mission == null) {
                return;
            }

            $.ajax({
                url  : "api/missions/" + mission + "/subscriptions",
                type : "GET",
                async : false,
                cache : false,
                contentType : false,
                processData : false,
                success: function (response) {
                    createMissionSubscriptionList(mission, response.data);
                },
                error : function(stat, err) {
                    $.jnotify("Error getting mission subscriptions", "error");
                }
            });
        }

        $(document).ready(function() {

            $("#send").click(function(event) {
                var formData = $('#missionSendForm').serialize();
                //alert(formData);
                $.ajax({
                    url  : 'api/missions/' + $.urlParam('name') + '/send',
                    type : 'POST',
                    data : formData,
                    contentType : 'application/x-www-form-urlencoded',
                })
                    .done(function() {
                        window.location = "Missions.jsp";
                    })
                    .fail(function() {
                        $.jnotify("Failed to send mission package", "error");
                    });
            });

        });

    </script>

</head>
<body onload="loadMission();loadMissionContacts();">
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

<h1>Send Mission as Mission Package</h1>

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
            <th>Subscriptions</th>
        </tr>
    </table>
</form>

<h2>Send mission to:</h2>

<form id="missionSendForm">
    <div id="missionContacts"></div>
    <br>
    <input type="button" id="send" value="Send" />
    <br>
    <br>
</form>

<a href="Missions.jsp">Mission Manager</a>
<br><br>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
