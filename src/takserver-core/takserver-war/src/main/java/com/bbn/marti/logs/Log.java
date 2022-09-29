/*
 */

package com.bbn.marti.logs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
	
	protected static final Logger logger = LoggerFactory.getLogger(Log.class);

	private int id;
    private String uid;
    private String callsign;
    private String platform;
    private String majorVersion;
    private String minorVersion;
    private String log;
    private byte[] contents;
    private String filename;
    private List<String> callstacks;
    private Timestamp time;
    private  HashMap<String, String> stackToExceptionMap = new HashMap<String, String>();

    public Log() { }
        
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }   
    
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }    

    public String getCallsign() {
        return callsign;
    }
    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }    
    
    public String getPlatform() {
        return platform;
    }
    public void setPlatform(String platform) {
        this.platform = platform;
    }    
    
    public String getMajorVersion() {
        return majorVersion;
    }
    public void setMajorVersion(String majorVersion) {
        this.majorVersion = majorVersion;
    }     
    
    public String getMinorVersion() {
        return minorVersion;
    }
    public void setMinorVersion(String minorVersion) {
        this.minorVersion = minorVersion;
    }     

//    public String getLog() {
//        return log;
//    }
    public void setLog(String log) {
        this.log = log;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getException(String callstack) {
        return stackToExceptionMap.get(callstack );
    }

    public Timestamp getTime() {
        return time;
    }
    public void setTime(Timestamp time) {
        this.time = time;
    }

    public List<String> getCallstacks() {
        return callstacks;
    }

    public void storeCallstacks() {

        if (log == null) {
            return;
        }

        try {
            String callstack;
            List<String> callstacks = new ArrayList<String>();

            if (log.startsWith("{")) {

                JSONParser parser = new JSONParser();
                Object parsedObj = parser.parse(log);
                if (parsedObj instanceof JSONObject) {
                    JSONObject json = ((JSONObject) parsedObj);
                    JSONObject report = (JSONObject)json.get("report");
                    callstack = (String)report.get("STACK_TRACE");

                    // store the exception
                    int nextLine = callstack.indexOf("\n");
                    String exception = callstack.substring(0, nextLine);

                    // trim off the exception from the callstack
                    callstack = callstack.substring(nextLine, callstack.length());

                    stackToExceptionMap.put(callstack, exception);

                    callstacks.add(callstack);
                }

            } else {
                int stackTrace = log.indexOf("STACK_TRACE=");
                if (stackTrace != -1) {
                    int nextLine = log.indexOf("\n", stackTrace);

                    String exception = log.substring(stackTrace + "STACK_TRACE=".length(), nextLine);

                    callstack = log.substring(nextLine, log.indexOf(",", nextLine));
                    callstack = callstack.replace("\n", "<br>");

                    stackToExceptionMap.put(callstack, exception);

                    callstacks.add(callstack);
                } else {
                    try {
                        int nextTab = log.indexOf("\t", 0);
                        while (nextTab != -1) {

                            String exception = log.substring(log.lastIndexOf("\n", nextTab - 2), nextTab);

                            int extraNewline = log.indexOf("\n\n", nextTab);
                            callstack = log.substring(nextTab, extraNewline) + "<br><br>";
                            callstack = callstack.replace("\n", "<br>");

                            stackToExceptionMap.put(callstack, exception);

                            callstacks.add(callstack);

                            nextTab = log.indexOf("\t", extraNewline + 1);
                        }
                    } catch (StringIndexOutOfBoundsException se) {
                        logger.error("Unable to parse callstack from log : " + getId());
                    }
                }
            }

            this.callstacks = callstacks;

        } catch (Exception e) {
            logger.error("Exception in storeCallstacks! id : " + id, e);
        }
    }
}
