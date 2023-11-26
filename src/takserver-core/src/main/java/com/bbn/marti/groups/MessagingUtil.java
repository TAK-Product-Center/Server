package com.bbn.marti.groups;

import java.util.List;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.Subscription;

import tak.server.cot.CotEventContainer;

public interface MessagingUtil {

	void sendLatestReachableSA(User destUser);
	
	List<CotEventContainer> getLatestSAForHandler(ChannelHandler handler);

	void processFederateClose(ConnectionInfo connection, ChannelHandler handler, Subscription subscription);

	void sendDisconnectMessage(Subscription subscription, ConnectionInfo connection);
	
	void sendDisconnect(CotEventContainer lastSA, Subscription subscription);

	void sendDeliveryFailure(String senderUid, CotEventContainer c);
}
