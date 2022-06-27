

package com.bbn.marti.groups;

import java.io.Serializable;

import org.springframework.http.HttpStatus;

public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = -5970513738563596437L;

    private HttpStatus status;

    private Long code;

    private String message;

    public ErrorResponse(HttpStatus status, Long code, String message) {
        super();
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public ErrorResponse() { }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public Long getCode() {
        return code;
    }

    public void setCode(Long code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ErrorResponse other = (ErrorResponse) obj;
        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (status != other.status)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ErrorResponse [status=" + status + ", code=" + code
                + ", message=" + message + "]";
    }
}
