

package com.bbn.cot.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
