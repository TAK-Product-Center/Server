package com.bbn.marti.tests;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created on 6/2/16.
 */
public abstract class ListRunner<T> {

    private static Random random = new Random();
    private final List<T> valueList = new LinkedList<>();
    private boolean randomize;

    public ListRunner(@Nullable List<T> initialValues, boolean randomize) {
        if (initialValues != null) {
            valueList.addAll(initialValues);
        }
        this.randomize = randomize;
    }

    public synchronized void add(T value) {
        valueList.add(value);
    }

    private synchronized static int randInt(int upperBound) {
        return random.nextInt(upperBound);
    }

    public abstract void doAction(T object);

    public synchronized int activate() {
        int listSize = valueList.size();

        if (listSize <= 0) {
            return 0;
        }

        int idx = (randomize ? randInt(listSize) : 0);

        T obj = valueList.remove(idx);

        doAction(obj);

        return valueList.size();
    }

}
