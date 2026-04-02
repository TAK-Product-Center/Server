

package com.bbn.marti.network;

import java.math.BigInteger;

public class CertificateSummary {

	public CertificateSummary() {}
	
	public CertificateSummary(String issuerDN, String subjectDN, BigInteger serialNumber, String fingerPrint,
			int maxHops, boolean allowTokenAuth, long tokenAuthDuration) {
		this.issuerDN = issuerDN;
		this.subjectDN = subjectDN;
		this.serialNumber = serialNumber;
		this.fingerPrint = fingerPrint;
		this.maxHops = maxHops;
		this.allowTokenAuth = allowTokenAuth;
		this.tokenAuthDuration = tokenAuthDuration;
	}
	
	public String getIssuerDN() {
		return issuerDN;
	}

	public void setIssuerDN(String issuerDN) {
		this.issuerDN = issuerDN;
	}
	
	public String getSubjectDN() {
		return subjectDN;
	}
	
	public void setSubjectDN(String subjectDN) {
		this.subjectDN = subjectDN;
	}
	
	public BigInteger getSerialNumber() {
		return serialNumber;
	}
	
	public void setSerialNumber(BigInteger serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getFingerPrint() {
		return fingerPrint;
	}

	public void setFingerPrint(String fingerPrint) {
		this.fingerPrint = fingerPrint;
	}

	public int getMaxHops() {
		return maxHops;
	}

	public void setMaxHops(int maxHops) {
		this.maxHops = maxHops;
	}

	public boolean isAllowTokenAuth() {
		return allowTokenAuth;
	}

	public void setAllowTokenAuth(boolean allowTokenAuth) {
		this.allowTokenAuth = allowTokenAuth;
	}

	public long getTokenAuthDuration() {
		return tokenAuthDuration;
	}

	public void setTokenAuthDuration(long tokenAuthDuration) {
		this.tokenAuthDuration = tokenAuthDuration;
	}


	private String issuerDN;
	private String subjectDN;
	private BigInteger serialNumber;
	private String fingerPrint;
	private int maxHops;
	private boolean allowTokenAuth;
	private long tokenAuthDuration;
}
