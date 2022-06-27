

package com.bbn.marti.nio.util;

import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.protocol.Protocol;

public class Protocols {
	public static <T> Runnable addProtocolListenerTask(final Protocol<T> protocol, final ProtocolListener<T> listener) {
        return new Runnable() {
            public void run() {
                protocol.addProtocolListener(listener);
            }
        };
	}
	
	public static <T> Runnable removeProtocolListenerTask(final Protocol<T> protocol, final ProtocolListener<T> listener) {
        return new Runnable() {
            public void run() {
                protocol.removeProtocolListener(listener);
            }
        };
	}
}