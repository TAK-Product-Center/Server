<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="com.bbn.marti.remote.config.CoreConfigFacade" %>
<%@ page import="com.bbn.marti.sync.FileList" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="com.bbn.marti.remote.util.SpringContextBeanForApi" %>
<%@ page import="tak.server.Constants" %>
<%@ page import="javax.naming.NamingException" %>
<%@ page import="javax.naming.InitialContext" %>

<html>
<head>
    <link rel="icon" type="image/x-icon" href="favicon.ico" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="icons/style-blue.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="tablesorter/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="css/bootstrap-theme.min.css" />
    <link rel="stylesheet" href="css/bootstrap.min.css">
    <link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
    <link rel="stylesheet" type="text/css" href="jquery/jquery.jnotify.css" />

    <script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
    <script type="text/javascript" src="jquery/jquery-ui.min.js"></script>
    <script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>

    <script type="text/javascript">



        function doDelete(dataString, resourceName) {
            $("#confirmDialog").dialog({
                buttons: {
                    "Delete" : function() {
                        $(this).dialog("close");
                        $.ajax({
                            type:"POST",
                            url:"sync/delete",
                            data: dataString,
                            success: function(data, textStatus, jqXHR) {
                                if (data.indexOf && data.indexOf("Authentication Required") > -1) {
                                    alert("You must log in to delete files.");
                                    return false;
                                }  else {
                                    $.jnotify("Deleted resource: " + resourceName);
                                    setTimeout(function() { window.location.reload(); }, 10);
                                }
                            },
                            error: function(jqXHR, textStatus, errorThrown) {
                                message = "Failed to delete file.";
                                if (errorThrown) {
                                    message = message + " Error: " + errorThrown;
                                }
                                $.jnotify(message, "error");
                            }
                        });
                    },
                    "Cancel" : function() {
                        $(this).dialog("close");
                    }
                }
            });

            var warning = "Do you want to delete " + resourceName + "? This cannot be undone!";

            $("#confirmDialogText").text(warning);
            $("#confirmDialog").dialog("open");
        }

        function setExpiration(resourceHash, expiration) {
            var url = "api/sync/metadata/" + resourceHash + "/expiration";
            var newDate = new Date(expiration);
            if (!isNaN(newDate)) {
                url += "?expiration=" + (newDate.getTime() - (60000 * newDate.getTimezoneOffset())) / 1000;
            } else {
                url += "?expiration=-1";
            }
            $("#expirationDialog").dialog({
                buttons: {
                    "Set": function() {
                        $(this).dialog("close");
                        $.ajax({
                            type:"PUT",
                            url:url,
                            success: function(data, textStatus, jqXHR) {
                                if (data.indexOf && data.indexOf("Authentication Required") > -1) {
                                    alert("You must log in to do this.");
                                    return false;
                                }  else {
                                    $.jnotify("Successfully set Expiration!");
                                }
                            }, error: function(jqXHR, textStatus, errorThrown) {
                                message = "Failed to set Expiration.";
                                if (errorThrown) {
                                    message = message + " Error: " + errorThrown;
                                }
                                $.jnotify(message, "error");
                            }
                        });
                    },
                    "Cancel": function() {
                        $(this).dialog("close");
                    }
                }
            });

            if (isNaN(newDate)) {
                expiration = "None";
            }
            $("#expirationDialogText").text("Do you want to set the expiration to:\n" + expiration);
            $("#expirationDialog").dialog("open");
        }

        $(document).ready(function()  {
            $("#confirmDialog").dialog({
                autoOpen : false,
                modal : true
            });

            $("#expirationDialog").dialog({
                autoOpen: false,
                modal: true
            })

            $(".delfile").click(function() {
                var toDelete = this.id;
                var resourceName = this.name;
                var dataString = "PrimaryKey="+toDelete;
                doDelete(dataString, resourceName);
            });
            $(".clearExpiration").click(function() {
                var toClear = this.id;
                console.log(toClear);
                console.log($("#" + toClear + ".expiration").val());
                $("#" + toClear + ".expirationVal").val(null);
            });
            $(".setExpiration").click(function() {
                var toSet = this.id;
                var hash = $("input[name=" + toSet + "]").val();
                var expiration = $("#" + toSet + ".expirationVal").val();
                console.log("hash: " + hash + ", expiration: " + expiration + ", toSet: " + toSet);
                setExpiration(hash, expiration);
            });
        });

        function selectAll() {
            $('input[type=checkbox]').each(function(){
                this.checked = !this.checked;
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

        function deleteSelected() {
            var selected = getSelected();
            if (selected.length == 0) {
                return;
            }

            var dataString = "";
            for (var i=0; i<selected.length; i++) {
                if (i != 0) {
                    dataString += "&";
                }
                dataString += "PrimaryKey=" + selected[i];
            }

            doDelete(dataString, selected.length + " files");
        }

    </script>

    <title>TAK Server Enterprise Sync</title>
</head>
<body>
<div id="header">
    <%@ include file="menubar.html" %>
	<%@ include file="init_audit_log.jsp" %>
</div>

<hr>
<%
	int uploadSizeLimitMB = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();
%>
<p><a href="upload.jsp">Upload a file (limit <%=uploadSizeLimitMB%> MB)</a></p>
    <a href="javascript:selectAll()">Select all</a>&nbsp;
    <a href="javascript:deleteSelected()">Delete selected</a>
</p>
<%
	ApplicationContext springContext = SpringContextBeanForApi.getSpringContext();
    FileList fileList = springContext.getBean(FileList.class);
    fileList.writeFilesHtml(out, request);
%>
<hr />
<div id="confirmDialog" title="Delete resource?" style="display: none;">
    <div id="confirmDialogText"></div>
</div>

<div id="expirationDialog" title="Set Expiration?" style="display: none">
    <div id="expirationDialogText"></div>
</div>

<%@ include file="footer.jsp" %>
</body>
</html>
