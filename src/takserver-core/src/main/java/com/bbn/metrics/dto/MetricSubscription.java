package com.bbn.metrics.dto;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.service.Subscription;

/**
 *
 */
public class MetricSubscription extends RemoteSubscription {
	private static final long serialVersionUID = -9010794387177623410L;
	private String callsign;
	private String clientUid;
	private int currentBandwidth;
	private int currentQueueDepth;
	private List<String> filterGroups;
	private GeospatialEventFilter geospatialEventFilter;
	private String handlerType;
	private boolean incognito;
	private AtomicBoolean isFederated;
	private AtomicLong lastProcTime;
	private String mode;
	private String notes;
	private AtomicInteger numHits;
	private boolean proxy;
	private int queueCapacity;
	private String role;
	private AtomicBoolean suspended;
	private String takv;
	private String team;
	private String uid;
	private AtomicLong writeQueueDepth;
	private String xpath;
	private String address;
	private String name;
	private String displayName;
	private AtomicLong totalTcpBytesWritten = new AtomicLong();
	private AtomicLong totalTcpBytesRead = new AtomicLong();
	private AtomicLong totalTcpNumberOfWrites = new AtomicLong();
	private AtomicLong totalTcpNumberOfReads = new AtomicLong();
	
	public MetricSubscription(Subscription subscription) {
		if (subscription != null) {
			this.callsign = subscription.callsign;
			this.clientUid = subscription.clientUid;
			this.currentBandwidth = subscription.currentBandwidth;
			this.currentQueueDepth = subscription.currentQueueDepth;
			this.filterGroups = subscription.filterGroups;
			this.geospatialEventFilter = subscription.geospatialEventFilter;
			this.handlerType = subscription.handlerType;
			this.incognito = subscription.incognito;
			this.isFederated = subscription.isFederated;
			this.lastProcTime = subscription.lastProcTime;
			this.mode = subscription.mode;
			this.notes = subscription.notes;
			this.numHits = subscription.numHits;
			this.queueCapacity = subscription.queueCapacity;
			this.role = subscription.role;
			this.suspended = subscription.suspended;
			this.takv = subscription.takv;
			this.team = subscription.team;
			this.uid = subscription.uid;
			this.writeQueueDepth = subscription.writeQueueDepth;
			this.xpath = subscription.xpath;
			
			if(subscription.getUser() != null) {
				this.address = subscription.getUser().getAddress();
				this.displayName = subscription.getUser().getDisplayName();
				this.name = subscription.getUser().getName();
			}
			
			if(subscription.getHandler() != null && subscription.getHandler() instanceof TcpChannelHandler) {
				TcpChannelHandler handler = (TcpChannelHandler) subscription.getHandler();
				this.totalTcpBytesRead = handler.totalTcpBytesRead;
				this.totalTcpBytesWritten = handler.totalTcpBytesWritten;
				this. totalTcpNumberOfWrites = handler.totalTcpNumberOfReads;
				this.totalTcpNumberOfReads = handler.totalTcpNumberOfWrites;
			}
		}
	}

	public String getCallsign() {
		return callsign;
	}

	public String getClientUid() {
		return clientUid;
	}

	public int getCurrentBandwidth() {
		return currentBandwidth;
	}

	public int getCurrentQueueDepth() {
		return currentQueueDepth;
	}

	public List<String> getFilterGroups() {
		return filterGroups;
	}

	public GeospatialEventFilter getGeospatialEventFilter() {
		return geospatialEventFilter;
	}

	public String getHandlerType() {
		return handlerType;
	}

	public boolean isIncognito() {
		return incognito;
	}

	public AtomicBoolean getIsFederated() {
		return isFederated;
	}

	public AtomicLong getLastProcTime() {
		return lastProcTime;
	}

	public String getMode() {
		return mode;
	}

	public String getNotes() {
		return notes;
	}

	public AtomicInteger getNumHits() {
		return numHits;
	}

	public boolean isProxy() {
		return proxy;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public String getRole() {
		return role;
	}

	public AtomicBoolean getSuspended() {
		return suspended;
	}

	public String getTakv() {
		return takv;
	}

	public String getTeam() {
		return team;
	}

	public String getAddress() {
		return address;
	}

	public String getUid() {
		return uid;
	}

	public AtomicLong getWriteQueueDepth() {
		return writeQueueDepth;
	}

	public String getXpath() {
		return xpath;
	}

	public AtomicLong getTotalTcpBytesWritten() {
		return totalTcpBytesWritten;
	}

	public AtomicLong getTotalTcpBytesRead() {
		return totalTcpBytesRead;
	}

	public AtomicLong getTotalTcpNumberOfWrites() {
		return totalTcpNumberOfWrites;
	}

	public AtomicLong getTotalTcpNumberOfReads() {
		return totalTcpNumberOfReads;
	}


	public String getName() {
		return name;
	}


	public String getDisplayName() {
		return displayName;
	}

	
	@Override
	public String toString() {
		return "MetricSubscription [callsign=" + callsign + ", clientUid=" + clientUid + ", currentBandwidth="
				+ currentBandwidth + ", currentQueueDepth=" + currentQueueDepth + ", filterGroups=" + filterGroups
				+ ", geospatialEventFilter=" + geospatialEventFilter + ", handlerType=" + handlerType +
				", incognito=" + incognito + ", isFederated=" + isFederated + ", lastProcTime="
				+ lastProcTime + ", mode=" + mode + ", notes=" + notes + ", numHits=" + numHits + ", proxy=" + proxy
				+ ", queueCapacity=" + queueCapacity + ", role=" + role + ", suspended=" + suspended + ", takv=" + takv
				+ ", team=" + team + ", to=" + to + ", uid=" + uid + ", writeQueueDepth=" + writeQueueDepth + ", xpath="
				+ xpath + ", totalTcpBytesWritten=" + totalTcpBytesWritten + ", totalTcpBytesRead=" + totalTcpBytesRead
				+ ", totalTcpNumberOfWrites=" + totalTcpNumberOfWrites + ", totalTcpNumberOfReads="
				+ totalTcpNumberOfReads + "]";
	}
}
