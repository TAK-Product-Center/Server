package com.bbn.marti.takcl.connectivity;

import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RunnableDockerServer extends AbstractRunnableServer {
	public RunnableDockerServer(AbstractServerProfile serverIdentifier) {
		super(serverIdentifier);
	}

	@Override
	protected void innerStopServer() {
		throw new NotImplementedException();
	}

	@Override
	protected void innerConfigureServer(@Nullable String sessionIdentifier, boolean enableRemoteDebug) {
		throw new NotImplementedException();
	}

	@Override
	protected void innerDeployServer(@Nullable String sessionIdentifier, long startTimeMs, boolean enableRemoteDebug) {
		throw new NotImplementedException();
	}

	@Override
	protected void innerKillServer() {
		throw new NotImplementedException();
	}

	@Override
	protected boolean isServerProcessRunning() {
		throw new NotImplementedException();
	}
}
