package com.bbn.marti.remote.socket;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

/*
 * 
 * POJO for control messages over WebSockets. Such as telling the client to subscribe to a new topic when the session expires.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // null fields will be not be included in the JSON serialized form
public class ControlMessage<T> extends TakMessage {
	
	private static final String className = ControlMessage.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}

    private static final long serialVersionUID = -2348988960672630084L;

    public ControlMessage(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("message kind must be specified");
        }
        
        command = kind;
        timestamp = new Date();
    }

    private Date timestamp;

    // message type
    private Kind command;

    // optional payload
    private T data;
    
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Kind getCommand() {
        return command;
    }

    public void setCommand(Kind command) {
        this.command = command;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    
    public String getMessageType() {
        return className;
    }
      
    @Override
    public int hashCode() {
        final int prime = 53;
        int result = 1;
        result = prime * result + ((command == null) ? 0 : command.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result
                + ((timestamp == null) ? 0 : timestamp.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ControlMessage other = (ControlMessage) obj;
        if (command != other.command)
            return false;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ControlMessage [timestamp=");
        builder.append(timestamp);
        builder.append(", command=");
        builder.append(command);
        builder.append(", data=");
        builder.append(data);
        builder.append("]");
        return builder.toString();
    }

    // message type
    public static enum Kind {
        REFRESH_TOPIC
    }
}


