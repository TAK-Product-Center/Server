

package com.bbn.marti.nio.binder;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;

import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;

public interface BinderFactory {
    public ServerBinder instance(
        int port,
        OrderedExecutor boundExecutor,
        StreamInstantiator streamInstantiator, 
        List<NetworkInterface> interfs,
        InetAddress group
    );
}