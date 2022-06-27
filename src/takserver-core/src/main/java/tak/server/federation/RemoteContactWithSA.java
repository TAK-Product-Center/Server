package tak.server.federation;

import com.bbn.marti.remote.RemoteContact;

import tak.server.cot.CotEventContainer;

public class RemoteContactWithSA extends RemoteContact {

	private static final long serialVersionUID = -6098452853334434548L;

	private CotEventContainer lastSA;

	public CotEventContainer getLastSA() {
		return lastSA;
	}

	public void setLastSA(CotEventContainer lastSA) {
		this.lastSA = lastSA;
	}
}
