<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="com.bbn.marti.remote.util.SpringContextBeanForApi" %>
<%@ page import="com.bbn.marti.CotImageBean" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
<link rel="icon" type="image/x-icon" href="favicon.ico" />
<link rel="stylesheet" href="css/bootstrap-theme.min.css" />
<link rel="stylesheet" href="css/bootstrap.min.css">
<title>TAK Server Database Information and Administration Interface</title>
<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
<script type="text/javascript" src="jquery/jquery-ui.js"></script>
<script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>
<script type="text/javascript">
  $(document).ready(function() 
	  { 
       $("#VacuumDb").click(function() {
                        var datastring = "vacuum";
       					$.ajax({
                              type:"POST",
                              url:"DBAdmin",
                              data: datastring,
                              success: function() {
                                 $.jnotify("Successfully sent vacuum request to DB");
                              },
                              error: function(jqXHR, textStatus, errorThrown) {
                                 $.jnotify("Failed to vacuum DB because "+errorThrown, "error");
                              }
                          });
       					return false;
	       });
       $("#ReindexDb").click(function() {
                        var datastring = "reindex";
       					$.ajax({
                              type:"POST",
                              url:"DBAdmin",
                              data: datastring,
                              success: function() {
                                 $.jnotify("Successfully sent re-index request to DB");
                              },
                              error: function(jqXHR, textStatus, errorThrown) {
                                 $.jnotify("Failed to re-index DB because "+errorThrown, "error");
                              }
                          });
       					return false;
	       });
      }
   );
</script>
</head>

<body>
<%@ include file="menubar.html"%>
<%
	try {
	ApplicationContext springContext = SpringContextBeanForApi.getSpringContext();
	CotImageBean cotBean = springContext.getBean(CotImageBean.class);
	int nCot = cotBean.getCountOfCoT();
	int nImage = cotBean.getCountOfImages();
%>
   <h2>Current Database Size</h2>
	  <p>Current number of various events in the database.</p>
		<table border="1">
			<tr>
				<td>CoT Events (estimate)</td>
				<td><%=nCot%></td>
			</tr>
			<tr>
			   <td>CoT Images (estimate)</td>
			   <td><%=nImage%></td>
			</tr>
		</table>
	<br>
 	<FORM id="VacuumDbForm">
	<input type="submit" value="Vacuum DB" id="VacuumDb" />
    </FORM>
 	<FORM id="ReindexDbForm">
	<input type="submit" value="ReIndex DB" id="ReindexDb" />
    </FORM>

	<br>
	<br>
	<br>
<%
	} catch (Exception e) {
%>
	<h3> <Font COLOR="DC143C"> Error connecting to Database: </Font> </h3>
	<p> Make sure that the database server is running, then refresh this page </p>
<%
	}
%>


<%@ include file="footer.jsp"%>
</body>

</html>
