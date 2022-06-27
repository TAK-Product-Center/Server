package com.bbn.marti.remote;

import java.io.Serializable;

public class UIDResult implements Serializable {

	private static final long serialVersionUID = -5715252740743622209L;

	public UIDResult() {}
	
	public UIDResult(String uid, String callSign) {
		this.uid = uid;
		this.callSign = callSign;
	}
	
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getCallSign() {
		return callSign;
	}

	public void setCallSign(String callSign) {
		this.callSign = callSign;
	}

	private String uid;
	private String callSign;
}
