

/*
 * 
 * Generic class to contain a response to an API request, including metadata fields and a T data field which will contain the data in the response.  
 * 
 */

package com.bbn.marti.cot.search.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import tak.server.system.ApiDependencyProxy;

@JsonInclude(Include.NON_NULL)
public class ApiResponse<T> {
    private String version;
    private String type;
    private T data;
    private List<String> messages;
    private final String nodeId;
    
    public ApiResponse() {
    	super();
        this.nodeId = ApiDependencyProxy.getInstance().serverInfo().getServerId();
    }
    
    public ApiResponse(String nodeId) {
    	super();
        this.nodeId = nodeId;
    }
    
    public ApiResponse(String version, String type, T data) {
        this();
        this.version = version;
        this.type = type;
        this.data = data;
    }
    
    public ApiResponse(String nodeId, String version, String type, T data) {
        this(nodeId);
        this.version = version;
        this.type = type;
        this.data = data;
    }

    public ApiResponse(String version, String type, T data, List<String> messages) {
    	this();
        this.version = version;
        this.type = type;
        this.data = data;
        this.messages = messages;
    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    public List<String> getMessages() {
    	return messages;
    }
    public void setMessages(List<String> messages) {
    	this.messages = messages;
    }
    public String getNodeId() {
		return nodeId;
	}

	@Override
    public String toString() {
        return "ApiResponse [version=" + version + ", type=" + type + ", data=" + data + "]";
    }    
}
