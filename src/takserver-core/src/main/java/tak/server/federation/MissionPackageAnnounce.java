package tak.server.federation;

import java.util.List;

public class MissionPackageAnnounce {
	
	private List<String> callsigns;
	private List<String> uids;
	private String annoucement;
	public List<String> getCallsigns() {
		return callsigns;
	}
	public void setCallsigns(List<String> callsigns) {
		this.callsigns = callsigns;
	}
	public List<String> getUids() {
		return uids;
	}
	public void setUids(List<String> uids) {
		this.uids = uids;
	}
	public String getAnnoucement() {
		return annoucement;
	}
	public void setAnnoucement(String annoucement) {
		this.annoucement = annoucement;
	}
	@Override
	public String toString() {
		return "MissionPackageAnnounce [callsigns=" + callsigns + ", uids=" + uids + ", annoucement=" + annoucement
				+ "]";
	}
}
