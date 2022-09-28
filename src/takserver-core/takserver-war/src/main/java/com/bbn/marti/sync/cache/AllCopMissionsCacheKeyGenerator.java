package com.bbn.marti.sync.cache;

import java.lang.reflect.Method;
import java.util.NavigableSet;

import org.springframework.cache.interceptor.KeyGenerator;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;


public class AllCopMissionsCacheKeyGenerator implements KeyGenerator {
	  
    public Object generate(Object target, Method method, Object... params) {
    	
    	if (!(params[1] instanceof NavigableSet<?>)) {
    		throw new IllegalArgumentException("can't construct cache key for all mission - invalid group collection type");
    	}
    	
    	// method name
		StringBuilder keyBuilder = new StringBuilder();
    	keyBuilder.append(method.getName());

		// tool
		keyBuilder.append("_");
		keyBuilder.append(String.valueOf(params[0]));

		// groups
		NavigableSet<Group> groups = ((NavigableSet<Group>)params[1]);
		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
		keyBuilder.append("_");
		keyBuilder.append(groupVector);

		// path
		if (params[2] != null) {
			keyBuilder.append("_");
			keyBuilder.append((String) params[2]);
		}

		// page
		if (params[3] != null) {
			keyBuilder.append("_");
			keyBuilder.append((String.valueOf(params[3])));
		}

		//size
		if (params[4] != null) {
			keyBuilder.append("_");
			keyBuilder.append((String.valueOf(params[4])));
		}

    	return keyBuilder.toString();
    }
}