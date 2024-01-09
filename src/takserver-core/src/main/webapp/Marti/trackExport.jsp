<jsp:useBean id="cotBean" scope="page"
	class="com.bbn.marti.CotImageBean" />
<%@page import="java.util.*"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="java.io.*"%>
<%@page import="java.net.*"%>
<%@page import="java.sql.*"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="tak.server.Constants"%>
<%@page import="com.bbn.marti.JDBCQueryAuditLogHelper"%>
<%@page import="com.bbn.marti.CotImageBean"%>
<%@page import="com.atakmap.android.icons.Icon2525bTypeResolver" %>
<%@ page import="org.ocpsoft.prettytime.PrettyTime" %>
<%@ page import="com.bbn.security.web.SecurityUtils" %>
<%@ page import="com.bbn.security.web.MartiValidator" %>
<%@ page import="org.owasp.esapi.Validator" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="org.owasp.esapi.errors.IntrusionException" %>
<%@ page import="org.owasp.esapi.errors.ValidationException" %>
<%@ page import="com.bbn.marti.remote.util.SpringContextBeanForApi" %>
<%
	final String context = "trackExport.jsp";
	Logger log = Logger.getLogger(context);
    Validator validator = SpringContextBeanForApi.getSpringContext().getBean(Validator.class);
	SimpleDateFormat sqlDateFormat = new SimpleDateFormat(Constants.SQL_DATE_FORMAT);
	SimpleDateFormat cotDateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);

   // Parse input
   String uid = "none", color="FFFFFFFF"; 
   String uidArg = request.getParameter("uid");
   String sinceArg = request.getParameter("since");
   String format = request.getParameter("format");
   String colorArg = request.getParameter("color");

   if (validator != null) {
	   try {
	   	uid = validator.getValidInput(context, uidArg, MartiValidator.Regex.MartiSafeString.name(), 
	   			MartiValidator.DEFAULT_STRING_CHARS, true);
	   	format = validator.getValidInput(context, format, MartiValidator.Regex.MartiSafeString.name(), 
	   			MartiValidator.DEFAULT_STRING_CHARS, true);
	   } catch (ValidationException ex) {
		   response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe parameter value detected.");
		   return;
	   } catch (IntrusionException ex) {
		  	log.severe(ex.getMessage());
		  	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe parameter value detected.");
		  	return;
	   }
   } else {
	uid = uidArg;
	if(colorArg != null)
		color = colorArg;
   }
   // Default to KML
   if (format == null) 
      format = "kml";
   else
      format = format.toLowerCase();
   
	long sinceTimeMillis = 0;
	if(sinceArg != null) {
		try {
	sinceTimeMillis = Long.parseLong(sinceArg);
		} catch (NumberFormatException ex) {
	 response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad number format for parameter \"since\"");
	   return;
		}
	}
	
	// Execute the SQL Query
	String sqlString = 
		"SELECT ST_X(event_pt), ST_Y(event_pt), servertime " +
		"FROM cot_router WHERE ";
		if(sinceTimeMillis > 0) {
	sqlString += " servertime > ? AND ";
		}
		sqlString += "uid=? ORDER BY servertime ASC;";
		
		try (java.sql.Connection connection =  SpringContextBeanForApi.getSpringContext().getBean(javax.sql.DataSource.class).getConnection(); java.sql.PreparedStatement sqlQuery = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.JDBCQueryAuditLogHelper.class).prepareStatement(sqlString, connection)) {
		
		sqlQuery.setString(1, uid);
		try (ResultSet results = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.JDBCQueryAuditLogHelper.class).doQuery(sqlQuery)) {
		if(results == null || results.isClosed()) {
	// do proper error handling... later..
		}

   if (format.equals("kml")) {
      response.setContentType("text/xml");
//<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
%>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
<Document>
  <Style id="track">
      <IconStyle><Icon><href>icons/track0.png</href></Icon></IconStyle>
  </Style>
      <Placemark>
	<ExtendedData>
		<Data name="linestyle">
			<value>Arrows</value>
		</Data>
	</ExtendedData>
	<name>track_log_<%=ESAPI.encoder().encodeForHTML(uid)%></name>
	<styleUrl>#track</styleUrl>
	<gx:Track>
		<altitudeMode>clampToGround</altitudeMode>
<% 
	HashMap<String, String> iconMap = new HashMap<String, String>();
	int NUMBER_OF_COLUMNS = 3;
	Timestamp start=null, end=null;
	while(results.next()) {
	  final String errorMessage = "Unvalidated due to parse error";
	  String geom = errorMessage;

		try {
                       geom = "<gx:coord>" + results.getDouble(1) + " " + results.getDouble(2) + "</gx:coord>";

                       if (validator == null) {
                               end = results.getTimestamp(3);
                       } else {
                               end = results.getTimestamp(3);
                       } 
			if(start == null)
				start = end;
		} catch (IllegalArgumentException ex) {
			log.severe("SQL query returned result that failed sanitization: " + ex.getMessage());
		} 
		if(results.getDouble(1) == 0 && results.getDouble(2) == 0)
			continue; //ignore bogus values 

		String when = "<when>" + cotDateFormat.format(end) + "</when>";
%>		<%=when%>
		<%=geom%>
<%  %>
	</gx:Track>
	<TimeSpan>
		<begin><%=cotDateFormat.format(start)%></begin>
		<end><%=cotDateFormat.format(end)%></end>
	</TimeSpan>
      </Placemark>
</Document>
</kml>
<% } } } }%>

