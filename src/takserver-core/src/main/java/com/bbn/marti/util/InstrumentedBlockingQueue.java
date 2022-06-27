package com.bbn.marti.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bbn.marti.remote.QueueMetric;
import com.google.common.base.Strings;

/*
 * 
 * Decorator class that accepts a BlockingQueue, and automatically instruments it
 * 
 */
public class InstrumentedBlockingQueue<E> implements BlockingQueue<E> {
    
    private final BlockingQueue<E> backingQueue;
    
    private final QueueMetric metric = new QueueMetric();
    
    private final String name;
    
    public InstrumentedBlockingQueue(BlockingQueue<E> backingQueue, String name) {
        
        if (backingQueue == null) {
            throw new IllegalArgumentException("null backing queue");
        }
        
        this.backingQueue = backingQueue;
        
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty queue name");
        }
        
        this.name = name;
        
        metric.capacity.set(backingQueue.remainingCapacity());
    }

    @Override
    public E element() {
        
        return backingQueue.element();
    }

    @Override
    public E peek() {
        
        return backingQueue.peek();
    }

    @Override
    public E poll() {
        E result = backingQueue.poll();
        
        
        return result;
    }

    @Override
    public E remove() {
        E result = backingQueue.remove();
        updateMetric();
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends E> arg0) {
        boolean result = backingQueue.addAll(arg0);
        updateMetric();
        return result;
    }

    @Override
    public void clear() {
        backingQueue.clear();
        updateMetric();
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        return backingQueue.containsAll(arg0);
    }

    @Override
    public boolean isEmpty() {
        return backingQueue.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return backingQueue.iterator();
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        boolean result = backingQueue.removeAll(arg0);
        updateMetric();
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        boolean result = backingQueue.retainAll(arg0);
        updateMetric();
        return result;
    }

    @Override
    public int size() {
        return backingQueue.size();
    }

    @Override
    public Object[] toArray() {
        return backingQueue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        return backingQueue.toArray(arg0);
    }

    @Override
    public boolean add(E arg0) {
        boolean result = backingQueue.add(arg0);
        updateMetric();
        return result;
    }

    @Override
    public boolean contains(Object arg0) {
        return backingQueue.contains(arg0);
    }

    @Override
    public int drainTo(Collection<? super E> arg0) {
        int result = backingQueue.drainTo(arg0);
        updateMetric();
        return result;
    }

    @Override
    public int drainTo(Collection<? super E> arg0, int arg1) {
        int result = backingQueue.drainTo(arg0, arg1);
        updateMetric();
        return result;
    }

    @Override
    public boolean offer(E arg0) {
        boolean result = backingQueue.offer(arg0);
        updateMetric();
        return result;
    }

    @Override
    public boolean offer(E arg0, long arg1, TimeUnit arg2) throws InterruptedException {
        boolean result = backingQueue.offer(arg0, arg1, arg2);
        updateMetric();
        return result;
    }

    @Override
    public E poll(long arg0, TimeUnit arg1) throws InterruptedException {
        E result = backingQueue.poll(arg0, arg1);
        updateMetric();
        return result;
    }

    @Override
    public void put(E arg0) throws InterruptedException {
        backingQueue.put(arg0);
        updateMetric();
    }

    @Override
    public int remainingCapacity() {
        return backingQueue.remainingCapacity();
    }

    @Override
    public boolean remove(Object arg0) {
        boolean result = backingQueue.remove(arg0);
        updateMetric();
        return result;
    }

    @Override
    public E take() throws InterruptedException {
        E result = backingQueue.take();
        updateMetric();
        return result;
    }
    
    public String toString() {
        return name + " " + backingQueue.toString();
    }
    
    private void updateMetric() {
        metric.currentSize.set(backingQueue.size());
    }
}
