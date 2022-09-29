package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class DockerRunnableServer extends AbstractRunnableServer {
	public DockerRunnableServer(AbstractServerProfile serverIdentifier) {
		super(serverIdentifier);
		throw new NotImplementedException();
	}

	@Override
	protected void innerStopServer() {
		throw new NotImplementedException();
	}

	@Override
	protected void innerDeployServer(@Nullable String sessionIdentifier, boolean enableRemoteDebug) {
		throw new NotImplementedException();
	}

	@Override
	protected void innerKillServer() {
		throw new NotImplementedException();
	}

	@Override
	protected boolean isServerProcessRunning(boolean shouldBeOnline) {
		throw new NotImplementedException();
	}

	@Override
	public List<AbstractServerProcess> getEnabledServerProcesses() {
		throw new NotImplementedException();
	}

	@Override
	protected void collectFinalLogs() {
		throw new NotImplementedException();
	}
}
