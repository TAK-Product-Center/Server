package com.bbn.metrics.dto;

import java.util.concurrent.atomic.AtomicLong;

public class NetworkMetrics {
	
	AtomicLong bytesWritten = new AtomicLong();
	AtomicLong numWrites = new AtomicLong();
	AtomicLong bytesRead = new AtomicLong();
	AtomicLong numReads = new AtomicLong();
	long numClients;
	
	public AtomicLong getBytesWritten() {
		return bytesWritten;
	}
	
	public void setBytesWritten(AtomicLong bytesWritten) {
		this.bytesWritten = bytesWritten;
	}
	
	public AtomicLong getNumWrites() {
		return numWrites;
	}
	
	public void setNumWrites(AtomicLong numWrites) {
		this.numWrites = numWrites;
	}
	
	public AtomicLong getBytesRead() {
		return bytesRead;
	}
	
	public void setBytesRead(AtomicLong bytesRead) {
		this.bytesRead = bytesRead;
	}
	
	public AtomicLong getNumReads() {
		return numReads;
	}
	
	public void setNumReads(AtomicLong numReads) {
		this.numReads = numReads;
	}
	
	public long getNumClients() {
		return numClients;
	}
	
	public void setNumClients(long numClients) {
		this.numClients = numClients;
	}

	@Override
	public String toString() {
		return "NetworkMetrics [bytesWritten=" + bytesWritten + ", numWrites=" + numWrites + ", bytesRead=" + bytesRead
				+ ", numReads=" + numReads + ", numClients=" + numClients + "]";
	}
}
