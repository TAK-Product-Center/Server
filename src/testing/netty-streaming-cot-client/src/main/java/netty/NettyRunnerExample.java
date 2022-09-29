package netty;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.SimpleChannelInboundHandler;
import netty.NettyClient.ConnectionLostCallback;
import netty.NettyClient.ConnectionMeta;

public class NettyRunnerExample {
	private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	private ConnectionLostCallback callback = () -> {
		System.out.println("Connection Lost, Attempting to reconnect in 10 seconds");
		executorService.schedule(this::initClient, 10, TimeUnit.SECONDS);
	};
	
	public static void main(String[] args) throws Exception {
		NettyRunnerExample ex = new NettyRunnerExample();
		ex.initClient();
		
		while (true);
	}
	
	private void initClient() {
		try {
			ConnectionMeta cm = new ConnectionMeta();
			
			NettyClient client = new NettyClient();
			
			// IF USING THIS PROJECT AS A 3RD PARTY LIBRARY, PASTE NioNettyHandler CLASS
			// INTO YOUR CODE FOR FULL CONTROL OF READS/WRITES OR EXTEND SimpleChannelInboundHandler<byte[]>
			// AND INITIALZE THAT CLASS HERE INSTEAD
			NioNettyHandler handler = new NioNettyHandler(callback);
			
			NettyInitializer initializer = new NettyInitializer(cm, handler); 
			
			client.startClient(cm, initializer);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
