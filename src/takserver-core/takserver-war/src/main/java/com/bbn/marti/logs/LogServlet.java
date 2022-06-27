package com.bbn.marti.logs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.HttpParameterConstraints;
import com.bbn.security.web.MartiValidator;

public class LogServlet extends EsapiServlet {
	
	@Autowired
	private PersistenceStore persistenceStore;

	public enum QueryParameter {
		id,
		uid,
		callsign,
		platform,
		majorVersion,
		minorVersion
	}	
	
	protected Map<String, HttpParameterConstraints> requiredPostParameters;
	protected Map<String, HttpParameterConstraints> optionalPostParameters;
	protected Map<String, HttpParameterConstraints> requiredGetParameters;
	protected Map<String, HttpParameterConstraints> optionalGetParameters;
	private static final long serialVersionUID = 3520164783651907004L;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LogServlet.class);
		
	@Override
	protected void initalizeEsapiServlet()
	{
		logger.debug("initializeEsapiServlet");
		
		requiredPostParameters = new HashMap<String, HttpParameterConstraints>();
		optionalPostParameters = new HashMap<String, HttpParameterConstraints>();		
		requiredPostParameters.put(QueryParameter.uid.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		requiredPostParameters.put(QueryParameter.callsign.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		requiredPostParameters.put(QueryParameter.platform.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		requiredPostParameters.put(QueryParameter.majorVersion.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		requiredPostParameters.put(QueryParameter.minorVersion.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));

		requiredGetParameters = new HashMap<String, HttpParameterConstraints>();
		optionalGetParameters = new HashMap<String, HttpParameterConstraints>();		
		requiredGetParameters.put(QueryParameter.id.name(), new HttpParameterConstraints(
				MartiValidator.Regex.MartiSafeString, MartiValidator.LONG_STRING_CHARS));
	}
		
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
    	try {
        	initAuditLog(request);
    		
            Map<String, String[]> httpParameters = validateParams("Log", request, response,
            		requiredPostParameters, optionalPostParameters);

            if(httpParameters == null)
                return;
    		
	      	String uid = getParameterValue(httpParameters, QueryParameter.uid.name());
	      	String callsign = getParameterValue(httpParameters, QueryParameter.callsign.name());
	      	String platform = getParameterValue(httpParameters, QueryParameter.platform.name());
	      	String majorVersion = getParameterValue(httpParameters, QueryParameter.majorVersion.name());
	      	String minorVersion = getParameterValue(httpParameters, QueryParameter.minorVersion.name());
	      	String encodedString = IOUtils.toString(request.getInputStream());
	    	      		      	
	      	final int BUFFER = 2048;
	      	byte[] zip = Base64.decode(encodedString.getBytes());
	      	ByteArrayInputStream bis = new ByteArrayInputStream(zip);
	      	ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));

	      	// iterate across file in the zip archive
	      	ZipEntry entry;
	      	while((entry = zis.getNextEntry()) != null) {
	      		
	      		// extract the contents
		      	int count = -1;
	      		byte data[] = new byte[BUFFER];
		      	ByteArrayOutputStream bos = new ByteArrayOutputStream();
  	            while ((count = zis.read(data, 0, BUFFER)) != -1) {
  	               bos.write(data, 0, count);
  	            }
  	            String fileContents = new String(bos.toByteArray(), "UTF-8");

  	            // create a Log object and store in the db
  	            Log log = new Log();
				log.setUid(uid);
				log.setCallsign(callsign);
				log.setPlatform(platform);
				log.setMajorVersion(majorVersion);
				log.setMinorVersion(minorVersion);
				log.setLog(fileContents);
				log.setFilename(entry.getName());
  	            persistenceStore.addLog(log);
  	              	            
  	            bos.flush();
  	            bos.close();
	      	}
	      	zis.close();
	      	
    	} 
    	catch (Exception e) {
    		logger.error("exception in doPost", e);
        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    	}
	    	
      	return;
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
        try {
        	initAuditLog(request);
        	
            Map<String, String[]> httpParameters = validateParams("Log", request, response,
            		requiredGetParameters, optionalGetParameters);

            if(httpParameters == null)
                return;
    		
	      	String id = getParameterValue(httpParameters, QueryParameter.id.name());
	      	String[] ids = id.split(",");

	      	//
	      	// return the single file in it's original form
	      	//
	      	if (ids.length == 1)
	      	{
	      		int logId = Integer.parseInt(ids[0]);
		      	Log log = persistenceStore.getLog(logId);
	
		        response.setContentType("text/plain");
		        int contentLength = log.getLog().length();
		        response.setContentLength(contentLength);
		        
				response.addHeader(
						"Content-Disposition", 
						"attachment; filename=" + ids[0] + "_" + log.getFilename());
		      	
		       response.getWriter().write(log.getLog());
	      	}
	      	
	      	//
	      	// return the selected files as a zip
	      	//
	      	else
	      	{
	      		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	      		BufferedOutputStream bos = new BufferedOutputStream(baos);
	      		ZipOutputStream zos = new ZipOutputStream(bos);

	      		for (String nextId : ids)
	      		{
		      		int logId = Integer.parseInt(nextId);
			      	Log log = persistenceStore.getLog(logId);
			      	if (log == null) {
			      		logger.error("Unable to find logId : " + logId);
			      		continue;
					}

      		        zos.putNextEntry(new ZipEntry(nextId + "_" + log.getFilename()));
      		        zos.write(log.getLog().getBytes());
      		        zos.closeEntry();
	      		}
      		    zos.close();
	      		
		        response.setContentType("application/zip");
		        int contentLength = baos.size();
		        response.setContentLength(contentLength);
		        
		        String date = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
		        String filename = "LogExport" + date + ".zip";
				response.addHeader(
						"Content-Disposition", 
						"attachment; filename=" + filename);	
		      	
		       response.getOutputStream().write(baos.toByteArray());
	      	}
	        
	    } catch (Exception e) {
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
        	persistenceStore.deleteLog(id);
        	
	    } catch (Exception e) {
	        logger.error("Exception!", e);
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    }        
    }    
}
