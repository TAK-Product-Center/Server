

package com.bbn.marti.util;

public interface Callbacks<T> {
	public void success(T info);
	public void error(String reason, Throwable trace);
}
