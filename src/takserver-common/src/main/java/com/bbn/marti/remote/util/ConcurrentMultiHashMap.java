package com.bbn.marti.remote.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/*
 * 
 * Since guava doesn't provide a concurrent Multimap, make one using a map of concurrent sets. The parts of this interface that we needed are implemented.
 * 
 * This uses a ConcurrentHashMap of ConcurrentSkipListSets.
 * 
 */

public class ConcurrentMultiHashMap<K, V> implements Multimap<K, V> {
    
    private Comparator<V> comparator = null;
    
    public void setComparator(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    private ConcurrentMap<K, Collection<V>> map = new ConcurrentHashMap<>();
    
    public ConcurrentHashMap<K, Collection<V>> getBackingMap() {
        return (ConcurrentHashMap<K, Collection<V>>) map;
    }

    @Override
    public Map<K, Collection<V>> asMap() {

        return map;
    }

    @Override
    public void clear() {
        map = new ConcurrentHashMap<>();
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
        // if we need this, we can iterate throw the value collection for a particular key.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {

        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object arg0) {

        // this would be slow
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Entry<K, V>> entries() {

        // would need to iterate through all of the entrysets
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> get(K key) {
        Collection<V> result = map.get(key);
        
        if (result != null) {
            return result;
        }
        
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Multiset<K> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean put(K key, V value) {
        
        Collection<V> newSet;
        
        // use the comparator if it's available
        if (comparator != null) {
            newSet = new ConcurrentSkipListSet<>(comparator);
        } else {
            newSet = new ConcurrentSkipListSet<>();
        }
        
        Collection<V> vals = null;
        
        // synchronized with remove
        synchronized (this) {
            // if there was no mapping for this key, vals will be null.
            vals = map.putIfAbsent(key, newSet);
        
            if (vals == null) {
                vals = newSet;
            }
        }
        
        return vals.add(value);
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> mmap) {
        
        throw new IllegalArgumentException();
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> vals) {
        
        throw new UnsupportedOperationException();
    }

    // synchronized with put of a new key
    @Override
    public synchronized boolean remove(Object key, Object value) {
        
        Set<V> vals = (Set<V>) map.get(key);
        
        if (vals == null) {
            return false;
        }

        if (vals.isEmpty()) {
            map.remove(key);
        }
        
        return vals.remove(value);
    }

    @Override
    public Collection<V> removeAll(Object key) {
        synchronized (this) {
            return map.remove(key);
        }
    }

    @Override
    public Collection<V> replaceValues(K arg0, Iterable<? extends V> arg1) {
        
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {

        int size = 0;

        for (Collection<V> v : map.values()) {
            size += v.size();
        }

        return size;
    }

    @Override
    public Collection<V> values() {
        
        throw new UnsupportedOperationException();
    }
}
