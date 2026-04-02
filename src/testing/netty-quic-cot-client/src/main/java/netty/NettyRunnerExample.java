package netty;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import netty.NettyClient.ConnectionLostCallback;

public class NettyRunnerExample {
	private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	
	public static void main(String[] args) throws Exception {
		NettyRunnerExample ex = new NettyRunnerExample();
		
//		 ex.initServer();
		
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.schedule(() -> {
			ex.initClient();
		}, 1, TimeUnit.SECONDS);
	}
	
	private void initClient() {
		try {			
			new NettyClient().startClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	private void initServer() {
		try {			
			NettyServer server = new NettyServer();
			server.buildQuicServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
