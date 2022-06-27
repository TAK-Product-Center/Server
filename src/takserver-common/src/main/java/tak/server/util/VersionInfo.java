package tak.server.util;

// Model class representing TAK server version
public class VersionInfo {
	
	private long major;
	private long minor;
	private long patch;
	private String branch;
	private String variant;
	

	public long getMajor() {
		return major;
	}
	public void setMajor(long major) {
		this.major = major;
	}
	public long getMinor() {
		return minor;
	}
	public void setMinor(long minor) {
		this.minor = minor;
	}
	public long getPatch() {
		return patch;
	}
	public void setPatch(long patch) {
		this.patch = patch;
	}
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public String getVariant() {
		return variant;
	}
	public void setVariant(String variant) {
		this.variant = variant;
	}
	
	
	
}
