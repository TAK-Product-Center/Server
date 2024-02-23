

package com.bbn.cot.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/*
 * 
 * value class representing an auth message
 * 
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "auth")
public class AuthMessage {
    
    private AuthCot cot;

    @XmlElement
    public AuthCot getCot() {
        return cot;
    }

    public void setCot(AuthCot cot) {
        this.cot = cot;
    }

    @Override
    public String toString() {
        return "AuthMessage [cot=" + cot + "]";
    }
}
