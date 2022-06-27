package com.bbn.metrics.dto;

import java.util.concurrent.atomic.AtomicLong;

public class QueueMetrics {

	private AtomicLong brokerSize;
	private AtomicLong brokerCapacity;
	private AtomicLong submissionSize;
	private AtomicLong submissionCapacity;
	private AtomicLong repositorySize;
	private AtomicLong repositoryCapacity;
	
	public AtomicLong getBrokerSize() {
		return brokerSize;
	}
	
	public void setBrokerSize(AtomicLong brokerSize) {
		this.brokerSize = brokerSize;
	}
	
	public AtomicLong getBrokerCapacity() {
		return brokerCapacity;
	}
	
	public void setBrokerCapacity(AtomicLong brokerCapacity) {
		this.brokerCapacity = brokerCapacity;
	}
	
	public AtomicLong getSubmissionSize() {
		return submissionSize;
	}
	
	public void setSubmissionSize(AtomicLong submissionSize) {
		this.submissionSize = submissionSize;
	}
	
	public AtomicLong getSubmissionCapacity() {
		return submissionCapacity;
	}
	
	public void setSubmissionCapacity(AtomicLong submissionCapacity) {
		this.submissionCapacity = submissionCapacity;
	}
	
	public AtomicLong getRepositorySize() {
		return repositorySize;
	}
	
	public void setRepositorySize(AtomicLong repositorySize) {
		this.repositorySize = repositorySize;
	}
	
	public AtomicLong getRepositoryCapacity() {
		return repositoryCapacity;
	}
	
	public void setRepositoryCapacity(AtomicLong repositoryCapacity) {
		this.repositoryCapacity = repositoryCapacity;
	}

	@Override
	public String toString() {
		return "QueueMetrics [brokerSize=" + brokerSize + ", brokerCapacity=" + brokerCapacity + ", submissionSize="
				+ submissionSize + ", submissionCapacity=" + submissionCapacity + ", repositorySize=" + repositorySize
				+ ", repositoryCapacity=" + repositoryCapacity + "]";
	}
}
