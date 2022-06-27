package com.bbn.metrics.dto;

import com.google.common.util.concurrent.AtomicDouble;

public class MemoryMetrics {
	
	private AtomicDouble heapCommitted;
	private AtomicDouble heapUsed;
	
	public AtomicDouble getHeapCommitted() {
		return heapCommitted;
	}
	
	public void setHeapCommitted(AtomicDouble heapCommitted) {
		this.heapCommitted = heapCommitted;
	}
	
	public AtomicDouble getHeapUsed() {
		return heapUsed;
	}
	
	public void setHeapUsed(AtomicDouble heapUsed) {
		this.heapUsed = heapUsed;
	}

	@Override
	public String toString() {
		return "MemoryMetrics [heapCommitted=" + heapCommitted + ", heapUsed=" + heapUsed + "]";
	}
}
