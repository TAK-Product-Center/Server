package com.bbn.marti.sync.cache;

import java.lang.reflect.Method;

import com.google.common.hash.Hashing;
import org.springframework.cache.interceptor.KeyGenerator;

public class MethodNameMultiStringArgCacheKeyGenerator implements KeyGenerator {
	  
    public Object generate(Object target, Method method, Object... params) {
    	
    	if (params.length < 1) {
    		throw new IllegalArgumentException("unable to compute cache key - no parameters");
    	}
    	
        StringBuilder keyBuilder = new StringBuilder();
        
        keyBuilder.append(method.getName());
        
        for (int i = 0; i < params.length; i++) {
        	keyBuilder.append("_");
            keyBuilder.append(Hashing.sha256().hashBytes(params[i].toString().getBytes()).toString());
        }

    	return keyBuilder.toString();
    }
}