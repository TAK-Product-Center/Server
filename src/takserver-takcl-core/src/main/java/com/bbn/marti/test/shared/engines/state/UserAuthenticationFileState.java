package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;

/**
 * Created on 2/9/18.
 */
public class UserAuthenticationFileState {

	private final AbstractServerProfile serverProfile;
	private UserAuthenticationFile previousServerUserAuthenticationFile;
	private UserAuthenticationFile currentServerUserAuthenticationFile;
	private UserAuthenticationFile localUserAuthenticationFile;

	public UserAuthenticationFileState(AbstractServerProfile serverProfile) {
		this.serverProfile = serverProfile;
		localUserAuthenticationFile = new UserAuthenticationFile();
	}

	public synchronized void updateCurrentServerAuthenticationFile() {
		previousServerUserAuthenticationFile = currentServerUserAuthenticationFile;
		currentServerUserAuthenticationFile = OfflineFileAuthModule.readCurrentConfigFromDisk(serverProfile);
	}

	public UserAuthenticationFile getCurrentServerUserAuthenticationFile() {
		return currentServerUserAuthenticationFile;
	}

	public UserAuthenticationFile getPreviousServerUserAuthenticationFile() {
		return previousServerUserAuthenticationFile;
	}


	public UserAuthenticationFile getLocalUserAuthenticationFile() {
		return localUserAuthenticationFile;
	}
}
