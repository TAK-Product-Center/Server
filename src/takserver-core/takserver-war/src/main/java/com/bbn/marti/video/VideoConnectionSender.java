/*
 * 
 * The VideoConnectionSender class sends Cot Alias events 
 * 
 */
package com.bbn.marti.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.spring.SpringContextBeanForApi;


//@WebServlet("/vcs/*")
public class VideoConnectionSender extends EsapiServlet { 
	
	private static final long serialVersionUID = -2364103405436785318L;

	@Autowired
	private VideoManagerService videoManagerService;

	@Autowired
	private SubmissionInterface submission;
	
    protected static final Logger logger = LoggerFactory.getLogger(VideoConnectionManager.class);
	
    private static String getCotMessage(String senderUid, String destUid, String address, String alias, 
    		String port, String roverPort, String rtspReliable, String ignoreEmbeddedKLV,
    		String path, String protocol, String networkTimeout, String bufferTime) {
    	
		String time = DateUtil.toCotTime(System.currentTimeMillis()); // now
		String staleTime = DateUtil.toCotTime(System.currentTimeMillis() + 3600000); // 1 hour from now
		String cot = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
			+ "<event version='2.0' uid='"+senderUid+"' type='b-i-v' "
			+ "time='"+time+"' start='"+time+"' stale='"+staleTime+"' how='m-g'>"
			+		"<point lat='0.0' lon='0.0' hae='0.0' ce='0.0' le='0.0' />"
			+ 		"<detail>"
			+			"<__video>"
			+ 				"<ConnectionEntry"
			+ 					" address='" + address + "'"
			+ 					" alias='" + alias + "'"
			+ 					" port='" + port + "'"
			+ 					" roverPort='" + roverPort + "'"
			+ 					" rtspReliable='" + rtspReliable + "'"
			+ 					" ignoreEmbeddedKLV='" + ignoreEmbeddedKLV + "'"
			+ 					" path='" + path + "'"
			+ 					" protocol='" + protocol + "'"
			+ 					" networkTimeout='" + networkTimeout + "'"
			+ 					" bufferTime='" + bufferTime + "'"
			+ 				"/>"
			+			"</__video>"
			+ 			"<marti><dest uid='" + destUid + "'/></marti>"
			+ 		"</detail>"
			+ "</event>";
 
		return cot;
    }    
    
    private static String[] getContacts(HttpServletRequest request) throws IOException, ServletException {
	    // Get the list of people to send an advertisement to (OPTIONAL)
    	String[] contacts = request.getParameterValues("contacts");
    	if(contacts == null || contacts.length == 0) {
    		List<String> contactList = new LinkedList<String>();
    			for(Part part : request.getParts()) {
    				if(part.getName().equalsIgnoreCase("contacts")) {
    					try(InputStream in = part.getInputStream()) {
							StringWriter writer = new StringWriter();
							IOUtils.copy(in, writer);
							contactList.add(writer.toString());
						}
    				}
    			}
    			contacts = contactList.toArray(new String[contactList.size()]);
	    	}
    	return contacts;
	 }    
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
    	try {
        	initAuditLog(request);

			CommonUtil martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);

			Objects.requireNonNull(martiUtil, "marti util bean");

			String groupVector = null;

			try {
				// Get group vector for the user associated with this session
				groupVector = martiUtil.getGroupBitVector(request);
				log.finer("groups bit vector: " + groupVector);
			} catch (Exception e) {
				log.fine("exception getting group membership for current web user " + e.getMessage());
			}

    	    String[] contacts = getContacts(request);
           	
           	String[] feedIds = request.getParameter("feedId").split("\\|");
        	for (String feedId : feedIds) {
        		Feed feed = videoManagerService.getFeed(Integer.parseInt(feedId), groupVector);
        		String senderUid = UUID.randomUUID().toString();

	           	for (String contact : contacts) {
	               	String cotMessage = getCotMessage(senderUid, contact, feed.getAddress(), feed.getAlias(), 
	               		feed.getPort(), feed.getRoverPort(), feed.getRtspReliable(), feed.getIgnoreEmbeddedKLV(),
	               		feed.getPath(), feed.getProtocol(), feed.getNetworkTimeout(), feed.getBufferTime());

	                submission.submitCot(cotMessage, martiUtil.getGroupsFromRequest(request));
	           	}
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
