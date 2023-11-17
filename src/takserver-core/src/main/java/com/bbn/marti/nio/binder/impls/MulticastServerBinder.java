

package com.bbn.marti.nio.binder.impls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.binder.BinderFactory;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.google.common.base.Joiner;

/**
* A server binder for opening and binding a multicast udp server datagram to a given port/group ip-addr/interface
*
* TODO: implement logic for rejoining the group and checking on the membership key (ie, does it expire or fail validation when
* we leave the multicast group)
*/
public class MulticastServerBinder extends UdpServerBinder {
	private final static Logger log = Logger.getLogger(MulticastServerBinder.class);

    /**
    * Returns a Multicast UDP data listener. Will bind to the given port, and push any accepted data into a channel handler
    * that has ... (see above for UDP server binder)
    *
    * The network interface and the inet address group are *not* ignored
    */
    public final static BinderFactory mudpBinderFactory = new BinderFactory() {
        public ServerBinder instance(
            int port,
            OrderedExecutor boundExecutor,
            StreamInstantiator streamInstantiator,
            List<NetworkInterface> interfs,
            InetAddress group)
        {
            return new MulticastServerBinder()
                .withInterfaces(interfs)
                .withGroup(group)
                .withPort(port)
                .withBoundExecutor(boundExecutor)
                .withInstantiator(streamInstantiator);
        }
    };


	private InetAddress group; // the address of the multicast group we're joining
	private final List<NetworkInterface> interfs = new LinkedList<NetworkInterface>(); // the list of network interfaces that we're going to try and join the group on

	public final MulticastServerBinder withGroup(InetAddress group) {
		Assertion.notNull(group);
        Assertion.pre(group.isMulticastAddress(), "Address given to multicast network input does not in fact support multicast");

		this.group = group;
		return this;
	}

	public final MulticastServerBinder withInterface(NetworkInterface interf) {
		Assertion.notNull(interf);

		try {
			if (interf.supportsMulticast()) {
				this.interfs.add(interf);
			} else {
				log.warn("Given network interface claims to not support multicast -- excluding from interfaces list: " + interf);
			}
		} catch (Exception e) {
			log.error("Error encountered trying to determine multicast properties of network interface -- excluding from multicast list: " + interf, e);
		}

		return this;
	}

	public MulticastServerBinder withInterfaces(List<NetworkInterface> interfs) {
		Assertion.notNull(interfs);

		for (NetworkInterface interf : interfs) {
			withInterface(interf);
		}

		return this;
	}

	/**
	* Either returns the list of user-specified network interfaces for
	* multicast, if nonempty, or the list of all network interfaces that support
	* multicast, if empty.
	*/
	private List<NetworkInterface> interfsOrAllMulticastInterfs() {
        if (!this.interfs.isEmpty()) {
            return this.interfs;
        } else {
            return NetUtils.allMulticastInterfs();
        }
	}

    @Override
    protected DatagramChannel doBind() throws IOException {
        DatagramChannel serverChannel = super.doBind();

        if (serverChannel != null) {
            // TODO: do something with the membership keys
            List<MembershipKey> joinedKeys = joinInterfaces(serverChannel);
        }

        return serverChannel;
    }

    private List<MembershipKey> joinInterfaces(DatagramChannel serverChannel) throws IOException {
        List<NetworkInterface> toJoin = interfsOrAllMulticastInterfs();
        List<MembershipKey> joinedKeys = new LinkedList<MembershipKey>();

        if (!toJoin.isEmpty()) {
            for (NetworkInterface interf : toJoin) {
                try {
                    MembershipKey key = serverChannel.join(group, interf);
                    joinedKeys.add(key);
                } catch (IOException e) {
                    log.warn(String.format("%s encountered io exception encountered trying to join %s", this, interf));
                }
            }

            if (joinedKeys.isEmpty()) {
                throw new IOException(this + " could not join any network interfaces that support multicast");
            }
        } else {
            log.warn(this + " has no available network interfaces that support multicast");
        }

        return joinedKeys;
    }

    @Override
	public String toString() {
		return String.format("[Multicast UDP server instantiator -- group: %s interfaces: [%s] local_address: %d]",
            group,
            Joiner.on(",").join(interfs),
            port()
        );
	}
}
