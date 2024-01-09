

package com.bbn.marti.nio.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.Assertion;


import com.bbn.marti.remote.config.CoreConfigFacade;

public class NetUtils {
    private final static Logger log = Logger.getLogger(NetUtils.class);
    
    /**
    * Returns a list of all the network interfaces that claim to support multicast
    */
    public static List<NetworkInterface> allMulticastInterfs() {
        List<NetworkInterface> result = new LinkedList<NetworkInterface>();
        
        Enumeration<NetworkInterface> interfsEnum = null;
        try {
            interfsEnum = NetworkInterface.getNetworkInterfaces();
            while (interfsEnum.hasMoreElements()) {
                NetworkInterface finger = interfsEnum.nextElement();
                //log.error("  testing interface: " + finger.getName());
                try {
                    if (finger.supportsMulticast()) {
                        //log.error("  ADDING interface: " + finger.getName());
                        result.add(finger);
                    }
                } catch (IOException e) {
                    log.warn("Error trying to inspect multicast properties of network interface -- excluding from multicast list: " + finger, e);
                }
            }
        } catch (IOException e) {
            log.warn("Error trying to inspect network interfaces", e);
            return result;
        }


                                
        return result;
    }
    
    /**
    * Opens an outgoing TCP channel, and sets it to be non-blocking. The connection process is started within
    * this method -- downstream, finishConnect must be called to complete the connection process. (With a 
    * non-blocking stream, a flag from the selector will indicate that all is well).
    */
    public static SocketChannel openTcpChannel(InetSocketAddress address) throws IOException {
        SocketChannel channel = null;
        
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(address);
        } catch (IOException e) {
            if (channel != null) {
                NetUtils.guardedClose(channel);
            }
            throw e;
        }
        
        return channel;
    }

    /**
    * Opens an outgoing (we are the client) UDP channel, and sets it to be non-blocking.
    */
    public static DatagramChannel openUdpChannel(InetSocketAddress address, String iface) throws IOException {
        DatagramChannel channel = null;
        
        try {
            channel = DatagramChannel.open();
            if (address.getAddress().isMulticastAddress()) {
                channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL,
                        CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getMulticastTTL());

                if (iface != null && !iface.isEmpty()) {
                    NetworkInterface networkInterface = NetworkInterface.getByName(iface);
                    if (networkInterface != null) {
                        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
                    } else {
                        log.error("exception setting mcast iface to : " + iface);
                    }
                }
            }

            channel.configureBlocking(false);
            channel.connect(address);
        } catch (IOException e) {
            if (channel != null) {
                NetUtils.guardedClose(channel);
            }
            throw e;
        }
        
        return channel;
    }

    public static int localPortOrNegativeOne(NetworkChannel channel) {
        Assertion.pre(channel != null);
    
        int localPort;
        try {
            InetSocketAddress address = (InetSocketAddress) channel.getLocalAddress();
            localPort = address.getPort();
        } catch (IOException e) {
            log.warn("Error retrieving local port for channel: " + channel);
            localPort = -1;
        }
    
        return localPort;
    }

    public static SelectionKey validSelectionKeyOrNull(SelectableChannel channel, Selector selector) {
        if (channel == null || selector == null) {
            log.warn("Asked to find selection key, but given null channel/selector");
            return null;
        } else {
            SelectionKey key = channel.keyFor(selector);
            
            if (key != null && key.isValid()) {
                return key;
            } else {
                return null;
            }
        }
    }

    public static void guardedForceClose(SelectableChannel channel, Selector selector) {
        SelectionKey key = channel.keyFor(selector);
        if (key != null) {
            // have a key -- try to reach in and close the channel handler
            NetUtils.guardedForceClose(key);
        } else {
            // have no key -- simply release the socket resource
            guardedClose(channel);
        }
    }

    public static void guardedForceClose(SelectionKey key) {
        if (key != null) {
            // retrieve things we want to close
            ChannelHandler handler = (ChannelHandler) key.attachment();
            SelectableChannel channel = key.channel();
    
            // do close on them (handler first, we want to give it the opportunity to close the channel itself)
            guardedForceClose(handler);
            guardedClose(channel);
        } else {
            log.warn("Asked to force close null selection key");
        }
    }

    public static void guardedClose(SelectableChannel toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException | RuntimeException e) {
                log.warn("IO exception while closing channel", e);
            }
        } else {
            log.warn("Asked to close null channel");
        }
    }

    public static void guardedForceClose(ChannelHandler toClose) {
        if (toClose != null) {
            try {
                toClose.forceClose();
            } catch (RuntimeException e) {
                log.warn("Exception while force closing channel handler", e);
            }
        } else {
            log.warn("Asked to force close null channel handler");
        }
    }
    
    public static void guardedForceClose(ConnectionInfo connection) {
        
        if (connection == null) {
            log.warn("can't force close null connection");
            return;
        }
        
        ChannelHandler toClose = null;
        
        toClose = (ChannelHandler) connection.getHandler();
        
        if (toClose != null) {
            try {
                toClose.forceClose();
            } catch (RuntimeException e) {
                log.warn("Exception while force closing channel handler", e);
            }
        } else {
            log.warn("can't force close null channel handler");
        }
    }
}
