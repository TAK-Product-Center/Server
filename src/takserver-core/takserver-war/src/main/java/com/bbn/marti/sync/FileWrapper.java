package com.bbn.marti.sync;

import java.io.Serializable;
import java.util.Arrays;

// Value class to hold a cached file, metadata and group vector
public class FileWrapper implements Serializable {
	
	private static final long serialVersionUID = 5243107224678618474L;
	
	byte[] contents;
	String hash;
	String uid;
	String groupVector;
	
	public byte[] getContents() {
		return contents;
	}
	public void setContents(byte[] contents) {
		this.contents = contents;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getGroupVector() {
		return groupVector;
	}
	public void setGroupVector(String groupVector) {
		this.groupVector = groupVector;
	}
	@Override
	public String toString() {
		return "FileHolder [contents=" + Arrays.toString(contents) + ", hash=" + hash + ", uid=" + uid
				+ ", groupVector=" + groupVector + "]";
	}

}
