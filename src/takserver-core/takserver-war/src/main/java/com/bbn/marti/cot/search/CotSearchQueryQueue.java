

package com.bbn.marti.cot.search;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bbn.marti.cot.search.model.CotSearch;

import tak.server.Constants;

/*
 * 
 * single spring-managed bean which contains a blocking queue. Usually will be singleton.
 * 
 * Decorator pattern - implicity decorated with the ability to be a spring-managed shared singleton component, and enforce genericness.
 * 
 */
public class CotSearchQueryQueue implements BlockingQueue<CotSearch> {
    
    private final BlockingQueue<CotSearch> q;
    
    public CotSearchQueryQueue() {
        q = new ArrayBlockingQueue<CotSearch>(Constants.COT_SEARCH_QUEUE_INITIAL_CAPACITY);
    }

    @Override
    public CotSearch remove() {
        return q.remove();
    }

    @Override
    public CotSearch poll() {
        return q.poll();
    }

    @Override
    public CotSearch element() {
        return q.element();
    }

    @Override
    public CotSearch peek() {
        return q.peek();
    }

    @Override
    public int size() {
       return q.size();
    }

    @Override
    public boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public Iterator<CotSearch> iterator() {
        return q.iterator();
    }

    @Override
    public Object[] toArray() {
        return q.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends CotSearch> c) {
        return q.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public boolean add(CotSearch e) {
       return q.add(e);
    }

    @Override
    public boolean offer(CotSearch e) {
        return q.offer(e);
    }

    @Override
    public void put(CotSearch e) throws InterruptedException {
      q.put(e);
    }

    @Override
    public boolean offer(CotSearch e, long timeout, TimeUnit unit) throws InterruptedException {
        return q.offer(e, timeout, unit);
    }

    @Override
    public CotSearch take() throws InterruptedException {
        return q.take();
    }

    @Override
    public CotSearch poll(long timeout, TimeUnit unit) throws InterruptedException {
        return q.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return q.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return q.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return q.contains(o);
    }

    @Override
    public int drainTo(Collection<? super CotSearch> c) {
        return q.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super CotSearch> c, int maxElements) {
        return q.drainTo(c, maxElements);
    }
}
