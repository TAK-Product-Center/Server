package com.bbn.marti.sync.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PropertiesService {

	List<String> findAllUids();
	
	Map<String, Collection<String>> getKeyValuesByUid(String uid);
	
	List<String> getValuesByKeyAndUid(String uid, String key);
	
	void putKeyValue(String uid, String key, String value);
	
	void deleteKey(String uid, String key);
	
	void deleteAllKeysByUid(String uid);
}
