package com.bbn.marti.sync.cache;

import java.lang.reflect.Method;
import java.util.NavigableSet;

import org.springframework.cache.interceptor.KeyGenerator;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;


public class InviteOnlyMissionCacheKeyGenerator implements KeyGenerator {

    public Object generate(Object target, Method method, Object... params) {

        if (!(params[2] instanceof NavigableSet<?>)) {
            throw new IllegalArgumentException("can't construct cache key for invite only missions - invalid group collection type");
        }

        // method name
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getName());

        // username
        keyBuilder.append("_");
        keyBuilder.append(String.valueOf(params[0]));

        // tool
        keyBuilder.append("_");
        keyBuilder.append(String.valueOf(params[1]));

        // groups
        NavigableSet<Group> groups = ((NavigableSet<Group>)params[2]);
        String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
        keyBuilder.append("_");
        keyBuilder.append(groupVector);

        return keyBuilder.toString();
    }
}