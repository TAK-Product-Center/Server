

package com.bbn.marti.util;

import com.bbn.marti.service.DistributedConfiguration;

/**
 * A simple class for asserting things
 *
 */
public class Assertion {
	
	private static final boolean throwOnAssertionFail = DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().isThrowOnAssertionFail();
	
    public static class AssertionException extends RuntimeException {

        private static final long serialVersionUID = 8931195410844601567L;

        public AssertionException(String msg) {
            super(msg);
        }
    }

    public static void pre(boolean toAssert) {
        condition(toAssert);
    }

    public static void pre(boolean toAssert, String msg) {
        condition(toAssert, msg);
    }

    public static void post(boolean toAssert) {
        condition(toAssert);
    }

    public static void post(boolean toAssert, String msg) {
        condition(toAssert, msg);
    }

    public static void condition(boolean toAssert) {
        condition(toAssert, "");
    }

    public static void condition(boolean toAssert, String msg) {
        if (throwOnAssertionFail && !toAssert)
            throw new AssertionException("Assertion.condition failure: " + msg);
    }

    public static void fail() {
        fail("");
    }

    public static void fail(String msg) {
        throw new AssertionException("Assertion.fail failure: " + msg);
    }

    public static <T> void areNotNull(Iterable<T> values) {
        for (T obj : values) {
            notNull(obj);
        }
    }

    public static void areNotNull(Object... values) {
        for (int i = 0; i < values.length; i++) {
            notNull(values[i]);
        }
    }


    public static void notNull(Object value, String msg) {
        if (value == null)
            throw new AssertionException("Assertion.notNull failure: " + msg);
    }

    public static void notNull(Object value) {
        notNull(value, "");
    }

    public static void isNull(Object value) {
        isNull(value, "");
    }

    public static void isNull(Object value, String msg) {
        if (value != null)
            throw new AssertionException("Assertion.null failure");
    }

    public static void zero(int zero) {
        if (zero != 0)
            throw new AssertionException("Assertion.zero failure");
    }

    public static void same(int i1, int i2) {
        if (i1 != i2) {
            throw new AssertionException(
                    String.format("Assertion.same failure: int 1 (%d) != int 2 (%d)", i1, i2)
                    );
        }
    }

    public static <T> void same(T obj1, T obj2) {
        Assertion.condition(obj1 == obj2);
    }
}