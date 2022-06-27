<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.bbn.marti.logging.AuditLogUtil" %>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Expires" CONTENT="-1">
    <title>Execution Checklist Manager</title>
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
    $(document).ready(function() {
        $("form[name=excheckForm]").submit(function(event) {
            event.preventDefault();
            var formData = new FormData($(this)[0]);
            $.ajax({
                url  : "api/excheck/template?clientUid=<%=AuditLogUtil.getUsername()%>&callsign=<%=AuditLogUtil.getUsername()%>",
                type : "POST",
                data : formData,
                async : true,
                cache : false,
                contentType : false,
                processData : false,
                success : function(returnData) {
                    //$.jnotify("Successfully uploaded template");
                    window.location.reload();
                },
                error : function(stat, err) {
                    $.jnotify.resume();
                    $.jnotify("Error submitting template", "error");
                }
            });
            return false;
        });
    });

    function createTemplatesTable(templatesMission) {
        var tbl = document.getElementById('templatesTable');
        var rowNumber = 0;
        for (var i = 0; i < templatesMission.contents.length; i++) {
            var row = tbl.insertRow(rowNumber + 1);
            rowNumber++;

            var cell = 0;

            var hash = sanitize(templatesMission.contents[i].data.hash);
            var name = templatesMission.contents[i].data.keywords[0];
            var description = templatesMission.contents[i].data.keywords[1];
            var createTime = sanitize(templatesMission.contents[i].data.submissionTime);
            var createdBy = templatesMission.contents[i].data.keywords.length > 2 ?
                templatesMission.contents[i].data.keywords[2] : templatesMission.contents[i].data.submitter;
            var checkbox = "<center><input type=\"checkbox\" name=\"templateCheckbox\" value=\"" + hash + "\" autocomplete=\"off\"/></center>";

            addCell(row, cell++, createDiv(checkbox), 'top');
            addCell(row, cell++, document.createTextNode(name), 'top');
            addCell(row, cell++, document.createTextNode(description), 'top');
            addCell(row, cell++, document.createTextNode(createTime), 'top');
            addCell(row, cell++, document.createTextNode(createdBy), 'top');

            var controls = "<input type=\"button\" id=\"delete\" value=\"Delete\" onClick=\"deleteHash('" + hash + "', true);\"/>";
            addCell(row, cell++, createDiv(controls), 'top');
        }
    }

    function getTemplates() {
        $.ajax({
            url  : "api/missions/ExCheckTemplates",
            type : "GET",
            async : false,
            cache : false,
            contentType : false,
            processData : false,
            success: function (response) {
                createTemplatesTable(response.data[0]);
            },
            error : function(stat, err) {
                $.jnotify("Error getting templates", "error");
            }
        });
    }

    function deleteHash(hash, showConfirm) {
        if (showConfirm && !confirm("Press Ok to delete template")) {
            return;
        }

        $.ajax({
            url  : "api/missions/ExCheckTemplates/contents?hash=" + hash + "&creatorUid=<%=ESAPI.encoder().encodeForURL(AuditLogUtil.getUsername())%>",
            type : "DELETE",
            async : false,
            cache : false,
            processData : false,
            success: function (response) {

                $.ajax({
                    url  : "sync/delete",
                    type : "POST",
                    async : false,
                    cache : false,
                    data : "hash=" + hash,
                    processData : false,
                    success: function (response) {
                        window.location.reload();
                    },
                    error : function(stat, err) {
                        $.jnotify("Error deleting template", "error");
                    }
                });

            },
            error : function(stat, err) {
                $.jnotify("Error deleting template", "error");
            }
        });
    }

    function stopChecklist(uid, showConfirm) {

        if (showConfirm && !confirm("Press Ok to delete the active checklist")) {
            return;
        }

        $.ajax({
            url  : "api/excheck/" + uid + "/stop?clientUid=<%=ESAPI.encoder().encodeForURL(AuditLogUtil.getUsername())%>",
            type : "POST",
            async : false,
            cache : false,
            processData : false,
            success: function (response) {
                if (showConfirm) {
                    window.location.reload();
                }
            },
            error : function(stat, err) {
                $.jnotify("Error deleting active checklist", "error");
            }
        });
    }

    function getSelected(name) {
        var selected = [];
        $('input[name=' + name + ']').each(function(){
            if (this.checked) {
                selected.push(this.value);
            }
        });

        return selected;
    }

    function deleteSelected(name) {

        if (!confirm("Press Ok to delete the selected items")) {
            return;
        }

        var selected = getSelected(name);
        if (selected.length == 0) {
            return;
        }

        for (var i=0; i<selected.length; i++) {
            if (name === 'checklistCheckbox') {
                stopChecklist(selected[i], false);
            } else if (name == 'templateCheckbox') {
                deleteHash(selected[i], false);
            }
        }

        window.location.reload();
    }

    function selectAll(name) {
        $('input[name=' + name + ']').each(function(){
            this.checked = !this.checked;
        });
    }

    function createChecklistsTable(checklists) {
        var tbl = document.getElementById('checklistsTable');
        var rowNumber = 0;

        $(checklists).find("checklist").each(function() {
            var row = tbl.insertRow(rowNumber + 1);
            rowNumber++;

            var checklistDetails = $(this).find("checklistDetails");
            var name = checklistDetails.find("name").text();
            var description = checklistDetails.find("description").text();
            var startTime = checklistDetails.find("startTime").text();
            var templateName = checklistDetails.find("templateName").text();
            var creatorCallsign = checklistDetails.find("creatorCallsign").text();

            var uid = checklistDetails.find("uid").text();
            var checkbox = "<center><input type=\"checkbox\" name=\"checklistCheckbox\" value=\"" + uid + "\" autocomplete=\"off\"/></center>";
            var controls = "<input type=\"button\" id=\"delete\" value=\"Delete\" onClick=\"stopChecklist('" + uid + "', true);\"/>";

            var cell = 0;
            addCell(row, cell++, createDiv(checkbox), 'top');
            addCell(row, cell++, document.createTextNode(name), 'top');
            addCell(row, cell++, document.createTextNode(description), 'top');
            addCell(row, cell++, document.createTextNode(startTime), 'top');
            addCell(row, cell++, document.createTextNode(templateName), 'top');
            addCell(row, cell++, document.createTextNode(creatorCallsign), 'top');
            addCell(row, cell++, createDiv(controls), 'top');
        });
    }

    function getChecklists() {
        $.ajax({
            url  : "api/excheck/checklist/active?clientUid=<%=ESAPI.encoder().encodeForURL(AuditLogUtil.getUsername())%>",
            type : "GET",
            dataType : "xml",
            async : false,
            cache : false,
            contentType : false,
            processData : false,
            success: function (response) {
                createChecklistsTable(response);
            },
            error : function(stat, err) {
                $.jnotify("Error getting checklists", "error");
            }
        });
    }


</script>


<body onload="getTemplates();getChecklists();">

<h1>Execution Checklist Manager</h1>

<form name="excheckForm">

    <table border=0 width=100%>
        <tr><td width=150 valign=top>File</td><td><input type="file" name="assetfile" size="50" /></td></tr>
        <tr><td width=150 valign=top>Name</td><td><input type="text" name="name" id="name" style="width:250px" /></td></tr>
        <tr><td width=150 valign=top>Description</td><td><input type="text" name="description" id="description" style="width:250px" /></td></tr>
        <tr><td><input type="submit" value="Add New Template"/></td></tr>
    </table>
    <br>

    <h2>Available Templates</h2>
    <input type="button" id="selectAllTemplates" value="Select All" onClick="selectAll('templateCheckbox');" />&nbsp;&nbsp;
    <input type="button" id="deleteSelectedTemplates" value="Delete Selected" onClick="deleteSelected('templateCheckbox');" />
    <br><br>
    <table id="templatesTable" border=1 cellpadding=5 cellspacing=1>
        <tr>
            <th>Select</th>
            <th>Name</th>
            <th>Description</th>
            <th>Create Time</th>
            <th>Created By</th>
            <th>Actions</th>
        </tr>
    </table>

    <h2>Active Checklists</h2>
    <input type="button" id="selectAllChecklists" value="Select All" onClick="selectAll('checklistCheckbox');" />&nbsp;&nbsp;
    <input type="button" id="deleteSelectedChecklists" value="Delete Selected" onClick="deleteSelected('checklistCheckbox');" />
    <br><br>
    <table id="checklistsTable" border=1 cellpadding=5 cellspacing=1>
        <tr>
            <th>Select</th>
            <th>Name</th>
            <th>Description</th>
            <th>Start Time</th>
            <th>Template Name</th>
            <th>Created By</th>
            <th>Actions</th>
        </tr>
    </table>

</form>

<hr />
<%@ include file="footer.jsp" %>
</body>
</html>
