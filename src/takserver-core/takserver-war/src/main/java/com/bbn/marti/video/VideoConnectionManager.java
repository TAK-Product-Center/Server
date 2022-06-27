/*
 * 
 * The VideoConnectionManager implements a RESTful interface for creating, reading, and deleting Feeds
 * 
 */

package com.bbn.marti.video;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.remote.util.SecureXmlParser;

public class VideoConnectionManager extends EsapiServlet {
	
	private static final long serialVersionUID = -1468567215815916730L;

	@Autowired
	private VideoManagerService videoManagerService;
	
    protected static final Logger logger = LoggerFactory.getLogger(VideoConnectionManager.class);

    private boolean addVideoConnections(HttpServletRequest request) throws JAXBException, IOException{

    	//String xml = org.apache.commons.io.IOUtils.toString(request.getInputStream());
		org.w3c.dom.Document doc = SecureXmlParser.makeDocument(request.getInputStream());
		if(doc != null) {
			JAXBContext jaxbContext = JAXBContext.newInstance(VideoConnections.class, Feed.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			VideoConnections videoConnections = (VideoConnections) jaxbUnmarshaller.unmarshal(doc);
			for (Feed feed : videoConnections.getFeeds()) {

				if (!feed.validate(validator)) {
					return false;
				}

				if (!videoManagerService.addFeed(feed, validator)) {
					return false;
				}
			}

			return true;
		}
		else{
			return false;
		}
    }
    
    private boolean setActive(HttpServletRequest request) {
    	
     	String feedId = request.getParameter("id");
     	String active = request.getParameter("active");
     	
		Feed feed = videoManagerService.getFeed(Integer.parseInt(feedId));
		feed.setActive(Boolean.parseBoolean(active));
		
		return videoManagerService.updateFeed(feed, validator);
    }
      
    /*
     * doPost accepts a VideoConnections object and adds each feed to the database
     */
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {

    	boolean success = false;
    	
        try {
        	initAuditLog(request);
        	
        	String action = request.getParameter("action");
        	if (action == null) {
        		action = "add";
        	}
 
        	if (action.equals("add")) {
            	success = addVideoConnections(request);
        	} else if (action.equals("setActive")) {
        		success = setActive(request);
        	}
	        
	    } catch (JAXBException e) {
	    	success = false;
	        e.printStackTrace();
	        logger.error("Exception!", e);
	    }        
        
        if (!success) {
        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /*
     * doGet returns a VideoConnections object that contains all feeds in the database
     */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
        try {
        	initAuditLog(request);
        	
        	JAXBContext jaxbContext = JAXBContext.newInstance(VideoConnections.class, Feed.class);
	        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	        
	        VideoConnections videoConnections = videoManagerService.getVideoConnections(false);
        	
        	StringWriter sw = new StringWriter();
        	jaxbMarshaller.marshal(videoConnections, sw);
        	String videoConnectionsXml = sw.toString();
        	
	        OutputStream outStream = response.getOutputStream();
	        outStream.write(videoConnectionsXml.getBytes());
	        outStream.close();
	        
	    } catch (JAXBException e) {
	        e.printStackTrace();
	        logger.error("Exception!", e);
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    }        
    }
    
    @Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
        try {
        	initAuditLog(request);
        	
        	String id = request.getParameter("id");
        	videoManagerService.deleteFeed(id);
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
