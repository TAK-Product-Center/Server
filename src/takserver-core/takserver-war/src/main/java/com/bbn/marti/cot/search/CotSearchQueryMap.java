

package com.bbn.marti.cot.search;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bbn.marti.cot.search.model.CotSearch;

/*
 * 
 * Decorator pattern - implicity decorated with the ability to be a spring-managed shared singleton component, and enforce  genericness.
 * 
 */

public class CotSearchQueryMap implements ConcurrentMap<String, CotSearch> {
    
    private final ConcurrentMap<String, CotSearch> map;
    
    public CotSearchQueryMap() {
        map = new ConcurrentHashMap<String, CotSearch>();
    }

    @Override
    public int size() {
       return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public CotSearch get(Object key) {
        return map.get(key);
    }

    @Override
    public CotSearch put(String key, CotSearch value) {
        return map.put(key, value);
    }

    @Override
    public CotSearch remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends CotSearch> m) {
        map.putAll(m);
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<CotSearch> values() {
        return map.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, CotSearch>> entrySet() {
        return map.entrySet();
    }

    @Override
    public CotSearch putIfAbsent(String key, CotSearch value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    @Override
    public boolean replace(String key, CotSearch oldValue, CotSearch newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Override
    public CotSearch replace(String key, CotSearch value) {
        return map.replace(key, value);
    }
}
