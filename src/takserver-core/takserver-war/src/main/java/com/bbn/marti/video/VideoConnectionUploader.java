/*
 * 
 * The VideoConnectionUploader class handles the POST request sent by the Add Video Feed UI
 * 
 */
package com.bbn.marti.video;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.video.Feed.Type;


//@WebServlet("/vcu/*")
public class VideoConnectionUploader extends EsapiServlet {
	
		private static final long serialVersionUID = 2500827655081259445L;

	@Autowired
	private VideoManagerService videoManagerService;

    protected static final Logger logger = LoggerFactory.getLogger(VideoConnectionManager.class);
	
    private String getParameter(HttpServletRequest request, String parameter) {
    	String value = request.getParameter(parameter);
    	if (value.length() == 0) {
    		return null;
    	}
    
    	return value;
    }
    
    private String getCheckboxParameter(HttpServletRequest request, String parameter, String on, String off) {
    	String value = request.getParameter(parameter);
    	return value != null && value.equals("on")? on : off;
    }
     
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
    	try {
        	initAuditLog(request);
        	
    		Feed feed = null;
        	String feedId = request.getParameter("feedId");
        	if (feedId == null || feedId.equals("null")) {
        		feed = new Feed();
        	} else {
        		feed = videoManagerService.getFeed(Integer.parseInt(feedId));
        	}
    		
        	feed.setUuid(getParameter(request, "uuid"));
    		feed.setActive(getCheckboxParameter(request, "active", "true", "false").equals("true"));
    		feed.setType(Type.VIDEO);
    		feed.setAlias(getParameter(request, "alias"));
    		
    		feed.setAddress(getParameter(request, "address"));
    		feed.setMacAddress(getParameter(request, "preferredMacAddress"));
    		feed.setPort(getParameter(request, "port"));
    		feed.setRoverPort(getParameter(request, "roverPort"));
    		feed.setIgnoreEmbeddedKLV(getCheckboxParameter(request, "ignoreEmbeddedKLV", "true", "false"));
    		feed.setPath(getParameter(request, "path"));
    		feed.setProtocol(getParameter(request, "protocol"));
    		feed.setNetworkTimeout(getParameter(request, "timeout"));
    		feed.setBufferTime(getParameter(request, "buffer"));
    		feed.setRtspReliable(getCheckboxParameter(request, "rtspReliable", "1", "0"));
    		
    		feed.setLatitude(getParameter(request, "latitude"));
    		feed.setLongitude(getParameter(request, "longitude"));
    		feed.setFov(getParameter(request, "fov"));
    		feed.setHeading(getParameter(request, "heading"));
    		feed.setRange(getParameter(request, "range"));

			feed.setThumbnail(getParameter(request, "thumbnail"));
			feed.setClassification(getParameter(request, "classification"));

    		if (!feed.validate(validator)
    		|| (feed.getId() == 0 && !videoManagerService.addFeed(feed, validator)) 
    		|| !videoManagerService.updateFeed(feed, validator))
    		{
    	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    			return;
    		}
    	
	    } catch (Exception e) {
	        e.printStackTrace();
	        logger.error("Exception!", e);
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    }
    }        
    		    
	@Override
	protected void initalizeEsapiServlet() {
		
	}
}
