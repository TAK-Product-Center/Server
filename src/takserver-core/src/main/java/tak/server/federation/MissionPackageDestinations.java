package tak.server.federation;

import java.util.List;

public class MissionPackageDestinations {
	
	private List<String> callsigns;
	private List<String> uids;
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
	@Override
	public String toString() {
		return "MissionPackageDestinations [callsigns=" + callsigns + ", uids=" + uids + "]";
	}
}
