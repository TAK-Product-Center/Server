package com.bbn.marti.test.shared.data.protocols;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 3/15/16.
 */
public class ProtocolProfileFilter {
	public Boolean protocolCanSend = null;
	public Boolean protocolCanReceive = null;

	public final ProtocolProfileFilter setCanSend(@Nullable Boolean canSend) {
		protocolCanSend = canSend;
		return this;
	}

	public final ProtocolProfileFilter setCanReceive(@Nullable Boolean canReceive) {
		protocolCanReceive = canReceive;
		return this;
	}

	public final ProtocolProfiles[] filterProtocolProfiles(ProtocolProfiles[] currentSet) {
		Set<ProtocolProfiles> returnSet = new HashSet<>();

		for (ProtocolProfiles protocol : currentSet) {
			if (doesConnectionMatch(protocol)) {
				returnSet.add(protocol);
			}
		}
		return returnSet.toArray(new ProtocolProfiles[0]);
	}

	public final boolean doesConnectionMatch(ProtocolProfiles protocol) {
		if (protocolCanSend != null && protocolCanSend != protocol.canSend()) {
			return false;
		} else if (protocolCanReceive != null && protocolCanReceive != protocol.canListen()) {
			return false;
		} else {
			return true;
		}
	}
}
