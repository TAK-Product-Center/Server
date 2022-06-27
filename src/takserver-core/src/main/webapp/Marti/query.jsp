<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
<link rel="icon" type="image/x-icon" href="favicon.ico" />
<link rel="stylesheet" href="css/bootstrap-theme.min.css" />
<link rel="stylesheet" href="css/bootstrap.min.css">
<title>CoT Query Interface</title>
<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>

</head>

<body>
	<%@ include file="menubar.html" %>
	<%@ include file="init_audit_log.jsp" %>
	
	<h1>CoT Query</h1>

	<form id="spatialQueryForm" name="spatialQueryForm" method="post" action="CotQueryServlet">
		<h2>Delivery Details (Required)</h2>
		<p>Please specify the IP address that will receive the query results, the delivery protocol, and 
		the maximum number of results desired.</p>
		<!-- Table-based layouts are for losers but I am too lazy to learn the proper CSS. -AG -->
		<table>
			<tr>
				<td valign="top">Host name or IP address: <input type="text" name="hostName" />
				<br />Port: <input type="text" name="port" value="1109" />
				</td>
			</tr>
			<tr>
				<td><input type="radio" name="protocol" value="tcp" checked />TCP<br />
				  <input type="radio" name="protocol" value="stcp" />Streaming TCP<br />
					<input type="radio" name="protocol" value="udp" />UDP (note: UDP transport may drop CoT events,
					   especially for full-size images or on a congested network)</td>
			</tr>
			<tr>
				<td>Maximum number of results: <input type="text" name="maximumResults" value="20" /></td>
		  </tr>
		  <tr>
		  <td>
		  	<input type="checkbox" name="replayMode" value="true"><label><strong>Replay Mode</strong></label>
		  	warp factor <input type="text" name="replaySpeed" value="1">
		  	</td>
		  </tr>	
		</table>
		
		   <h2>Metadata Search</h2>
	  <p>You can limit the search based on metadata attributes of CoT events.</p>
		<table border="1">
			<tr>
				<th>Property</th>
				<th>Type</th>
				<th>Value</th>
			</tr>
			<tr>
			   <td>Cot Type</td>
			   <td>regex</td>
            <td><input type="text" name="cotType" value =".*" /></td>
			<tr>
			   <td>UID</td>
			   <td>regex</td>
				<td><input type="text" name="uid" value=".*" /></td>
			</tr>
			<tr>
			   <td>Minimum start</td>
			   <td>timestamp</td>
			   <td><input type="text" name="minimumStartTime" /> 
			</tr>
			<tr>
			   <td>Maximum start</td>
            <td>timestamp</td>
            <td><input type="text" name="maximumStartTime" /> 
			</tr>
		</table>

   <p><label><input type="checkbox" name="latestByUid" value="true">Return only the latest event for each UID</label></p>
	
	   <h2>Geospatial Search</h2>
	     <p>You can search for CoT events within a defined geographic area. If you leave this section blank,
	     your query will match any CoT events worldwide.</p>
	     
	     <p>Choose whether you want to search a circular or rectangular region and enter the details. 
	     Latitude and longitude are in decimal degrees, with bounds of [-90, +90] and [-180, +180], respectively.
	     Radius is in meters as per the CoT protocol.</p>
      <table>
      <tr>
         <th colspan="2"><input type="radio" name="searchArea" value="CIRCLE" />Circular Region</th>
         <th colspan="2"><input type="radio" name="searchArea" value="RECTANGLE" />Rectangular Region</th>
      </tr>
      <tr>
         <td>Center Latitude:</td>
         <td><input type="text" name="centerLatitude" /></td>
         <td>Top Latitude (northern edge):</td>
         <td><input type="text" name="rectangleTop" /></td>
      </tr>
      <tr>
         <td></td>
         <td></td>
         <td>Bottom Latitude (southern edge):</td>
         <td><input type="text" name="rectangleBottom" /></td>
      </tr>
      <tr>
         <td>Center Longitude:</td>
         <td><input type="text" name="centerLongitude" />
         <td>Left Longitude (western edge):</td>
         <td><input type="text" name="rectangleLeft" />
      </tr>
      <tr>
         <td>Radius (meters):</td>
         <td><input type="text" name="radius" />
         <td>Right Longitude (eastern edge):</td>
         <td><input type="text" name="rectangleRight" />
      </tr>
      
      </table>
	   
   <br />
   <br />

   <p><input type="submit" value="Submit" /></p>
	</form>
	<%@ include file="footer.jsp" %>	
</body>

</html>
