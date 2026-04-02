package com.bbn.marti.service;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.dom4j.Attribute;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import atakmap.commoncommo.protobuf.v1.ContactOuterClass.Contact;
import atakmap.commoncommo.protobuf.v1.Cotevent.CotEvent;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

public class PluginBasedSubscription extends Subscription {

	private static final long serialVersionUID = 1L;
	
	public PluginBasedSubscription(CotEventContainer cotEvent) throws IOException {
		
		
		this.team = getSingleNodeValue(cotEvent, "/event/detail/__group/@name");
		this.role = getSingleNodeValue(cotEvent, "/event/detail/__group/@role");
	    
		this.callsign = cotEvent.getCallsign();
		this.uid = cotEvent.getUid();
		this.clientUid = cotEvent.getUid();
		this.xpath = "";
		this.takv = getSingleNodeValue(cotEvent, "/event/detail/takv/@platform") + ":" + getSingleNodeValue(cotEvent, "/event/detail/takv/@version");
		ChannelHandler dummyHandler = new AbstractBroadcastingChannelHandler() {
			
	        @Override
	        public AsyncFuture<ChannelHandler> close() {
	            return null;
	        }

	        @Override
	        public void forceClose() { }

	        @Override
	        public String netProtocolName() { 
	            return "";
	        }

	        @Override
	        public String toString() {
	            return "stcp:localhost:8080";
	        }
	    };
	    this.setEncoder(new AbstractBroadcastingProtocol<CotEventContainer>() {

            @Override
            public void onConnect(ChannelHandler handler) { }

            @Override
            public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) { }

            @Override
            public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
                return null;
            }

            @Override
            public void onInboundClose(ChannelHandler handler) { }

            @Override
            public void onOutboundClose(ChannelHandler handler) { }
            
        });
	    this.to = dummyHandler.toString();
		this.setHandler(dummyHandler);
		FederateUser subUser = new FederateUser(cotEvent.getUid(), cotEvent.getUid(), callsign, cotEvent.getEndpoint(), null, null, null);
		this.setUser(subUser);
				
	}
	
	private String getSingleNodeValue(CotEventContainer cotEvent, String attr) {
		Attribute callsignAttr = (Attribute) cotEvent.getDocument().selectSingleNode(attr);
		if (callsignAttr != null) {
			return callsignAttr.getValue();
		} else {
			return null;
		}
	}

}
