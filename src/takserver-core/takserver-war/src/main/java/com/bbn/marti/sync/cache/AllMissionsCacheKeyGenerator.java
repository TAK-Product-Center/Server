package com.bbn.marti.sync.cache;

import java.lang.reflect.Method;
import java.util.NavigableSet;

import org.springframework.cache.interceptor.KeyGenerator;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;

public class AllMissionsCacheKeyGenerator implements KeyGenerator {
	  
    public Object generate(Object target, Method method, Object... params) {
    	
    	if (!(params[3] instanceof NavigableSet<?>)) {
    		throw new IllegalArgumentException("can't construct cache key for all mission - invalid group collection type");
    	}
    	
    	@SuppressWarnings("unchecked")
		NavigableSet<Group> groups = ((NavigableSet<Group>)params[3]);
    	
        String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

        StringBuilder keyBuilder = new StringBuilder();
    	
    	keyBuilder.append(method.getName());
		keyBuilder.append("_");
		keyBuilder.append((boolean)params[0]);
		keyBuilder.append("_");
		keyBuilder.append((boolean)params[1]);

		if (params[2] != null) {
			keyBuilder.append("_");
			keyBuilder.append((String) params[2]);
		}

    	keyBuilder.append("_");
    	keyBuilder.append(groupVector);

    	return keyBuilder.toString();
    }
}