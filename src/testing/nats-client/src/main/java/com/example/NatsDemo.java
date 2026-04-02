package com.example;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import io.nats.client.Connection;
import io.nats.client.Nats;

public class NatsDemo {
	public static final String CLUSTER_DATA_MESSAGE = "takserver-data-message";
	
    public static void main(String[] args) throws Exception {
    	String natsUrl = "nats://localhost:4222";
        if (args.length == 1) {
        	natsUrl = args[0];
        }
        
        Connection nc = Nats.connect(natsUrl);
        new Thread(() -> {
        	try {
            	nc.createDispatcher().subscribe(CLUSTER_DATA_MESSAGE, m -> {
    				try {		
    					Message proto = Message.parseFrom(m.getData());
    					System.out.println("CLUSTER_DATA_MESSAGE: " + proto);
    				} catch (Exception e) {
    					System.out.println("exception processing clustered message: " + e);
    				}
    			});
    		} catch (Exception e) {
    			System.out.println("exception connecting to NATS server to receive messages: " + e);
    		}
        }).start();
        
        Thread.currentThread().join();
        nc.close();
    }
}
