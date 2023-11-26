package com.bbn.marti.sync.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;

public class AllMissionsCacheKeyGenerator implements KeyGenerator {
	  
    public Object generate(Object target, Method method, Object... params) {
    	
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

		return keyBuilder.toString();
    }
}