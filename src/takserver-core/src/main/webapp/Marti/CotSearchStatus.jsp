<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title>Marti CoT Query Status</title>

<link rel="stylesheet" href="tablesorter/style.css" type="text/css" media="print, projection, screen" />
<link type="text/css" href="jquery/jquery-ui.css" rel="stylesheet" media="all" />
<link type="text/css" href="jquery/jquery.jnotify.css" rel="stylesheet" media="all" />
<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script> 
<script type="text/javascript" src="jquery/jquery-ui.js"></script> 
<script type="text/javascript" src="tablesorter/jquery.tablesorter.min.js"></script> 
<script type="text/javascript" src="jquery/jquery.jnotify.min.js"></script>

<script type="text/javascript" src="jquery/jquery-3.5.0.js"></script>
<script type="text/javascript">
	var parseQueryString = function( queryString ) {
    	var params = {}, queries, temp, i, l;
 
	    // Split into key/value pairs
    	queries = queryString.split("&");
 
    	// Convert the array of strings into an object
    	for (i = 0, l = queries.length; i < l; i++) {
        	temp = queries[i].split('=');
        	params[temp[0]] = temp[1];
   	 	}	
 
	    return params;
	};
	
	$(document).ready(
			function() {

				var status = "";
				
				var queryString = window.location.search;
				queryString = queryString.substring(1);
				
				var params = parseQueryString(queryString);
				
				// set query id
				$('#query_id').text(params.queryId);
				
				var cotSearchUrl = "api/cot/search/" + params.queryId;
				
				// keep track of the polling timer id
				var intervalId;

				var pollQueryStatus = function() {
					$.getJSON(cotSearchUrl,
							function(data) {
								var cotSearch = data.data[0];
								
								console.log(cotSearch);

								var message = cotSearch.message;
								var count = cotSearch.count;
								var timestamp = new Date(cotSearch.timestamp);
								var active = cotSearch.active;
								var tag = cotSearch.tag;
								
								// update if the status has changed
								if (status !== cotSearch.status) {
									status = cotSearch.status;
									
									$(function() {
										var $tr = $('<tr>').append(
												$('<td>').text(status),
												$('<td>').text('' + timestamp),
												$('<td>').text(count),
												$('<td>').text(message)).appendTo('#query_status');
									});
									
									// When the query is finished processing, stop polling.
									if (!active) {
										clearInterval(intervalId);
									}
								}
							});
				};
				
				pollQueryStatus();

				// poll query status every 750 ms
				intervalId = setInterval(pollQueryStatus, 750);
			});
</script>

</head>
<body>
<%@ include file="menubar.html" %>
<%@ include file="init_audit_log.jsp" %>

	<div id="heading">
	<h3>Marti CoT Search Query <span id="query_id"></span></h3>
	
	<table id="query_status" class="tablesorter">
		<thead>
			<tr><th>Status</th><th>Time</th><th>Result Count</th><th>Message</th>
		</thead>
	<tbody></tbody>
	</table>
	
	<%@ include file="footer.jsp" %>
</body>
</html>
