

package com.bbn.marti.network;

import java.math.BigInteger;

public class CertificateSummary {

	public CertificateSummary() {}
	
	public CertificateSummary(String issuerDN, String subjectDN, BigInteger serialNumber, String fingerPrint, int maxHops) {
		this.issuerDN = issuerDN;
		this.subjectDN = subjectDN;
		this.serialNumber = serialNumber;
		this.fingerPrint = fingerPrint;
		this.maxHops = maxHops;
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

	private String issuerDN;
	private String subjectDN;
	private BigInteger serialNumber;
	private String fingerPrint;
	private int maxHops;
}
