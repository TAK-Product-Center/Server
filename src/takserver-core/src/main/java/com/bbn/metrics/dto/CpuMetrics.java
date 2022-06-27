package com.bbn.metrics.dto;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;

public class CpuMetrics {
	
	private AtomicInteger cpuCount;
	private AtomicDouble cpuUsage;	
	
	public AtomicInteger getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(AtomicInteger cpuCount) {
		this.cpuCount = cpuCount;
	}

	public AtomicDouble getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(AtomicDouble cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	@Override
	public String toString() {
		return "CpuMetrics [cpuCount=" + cpuCount + ", cpuUsage=" + cpuUsage + "]";
	}

}
