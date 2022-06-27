package com.bbn.marti.remote;

public interface ServerInfo {
	
	// unique identifer for this server
	String getServerId();
	
	// messaging topics for internal pub-sub
	String getSubmissionTopic();
	
	String getTakMessageTopic();
	
	boolean isCluster();
	
	String getNatsURL();
	
	String getNatsClusterId();

}
