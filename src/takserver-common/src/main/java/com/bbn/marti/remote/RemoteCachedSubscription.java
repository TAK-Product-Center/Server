

package com.bbn.marti.remote;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteCachedSubscription extends RemoteSubscription {

	private static final long serialVersionUID = -305117435954238582L;
	public String inboundVector = "";
	public String outboundVector = ""; 
	
	public RemoteCachedSubscription(List<?> row) {
		this.callsign = (String) row.get(0);
		this.clientUid = (String) row.get(1);
		this.setConnectionId((String) row.get(2));
		this.currentBandwidth = (int) row.get(3);
		this.handlerType = (String) row.get(4);
		this.iface = (String) row.get(5);
		this.incognito = (boolean) row.get(6);
		this.lastProcTime.set(((AtomicLong) row.get(7)).get());
		this.mode = (String) row.get(8);
		this.notes = (String) row.get(9);
		this.role = (String) row.get(10);
		this.takv = (String) row.get(11);
		this.team = (String) row.get(12);
		this.to = (String) row.get(13);
		this.uid = (String) row.get(14);
		this.setUsername((String) row.get(15));
		this.xpath = (String) row.get(16);
		this.numHits = (AtomicInteger) row.get(17);
		this.writeQueueDepth = (AtomicLong) row.get(18);
		this.lastProcTime = (AtomicLong) row.get(19);
		inboundVector = (String) row.get(20);
		outboundVector = (String) row.get(21);
		this.originNode = (UUID) row.get(22);
	}
	
}


