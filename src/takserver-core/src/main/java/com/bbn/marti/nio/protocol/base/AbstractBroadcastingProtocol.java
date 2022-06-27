

package com.bbn.marti.nio.protocol.base;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
 * An abstract class for handling notification and management of Protocol Listeners using rwlocks
 *
 */
public abstract class AbstractBroadcastingProtocol<T> implements Protocol<T> {
	private static final Logger log = LoggerFactory.getLogger(AbstractBroadcastingProtocol.class);
	private final ConcurrentLinkedQueue<ProtocolListener<T>> protocolListeners;

	protected AbstractBroadcastingProtocol() {
		this.protocolListeners = new ConcurrentLinkedQueue<ProtocolListener<T>>();
	}

	public abstract void onConnect(ChannelHandler handler);
	public abstract void onDataReceived(ByteBuffer buffer, ChannelHandler handler);
	public abstract AsyncFuture<Integer> write(T data, ChannelHandler handler);
	public abstract void onInboundClose(ChannelHandler handler);
	public abstract void onOutboundClose(ChannelHandler handler);

	@Override
	public final boolean addProtocolListener(ProtocolListener<T> listener) {
		Assertion.notNull(listener);

		// try catch for foolproof lock protection
		boolean added = false;
		try {
			if (containsProtocolListener(listener)) {
				return false;
			}

			added = this.protocolListeners.add(listener);
		} catch (Exception e) {
			log.error("Error adding protocol listener (" + listener + ") to listeners list for " + this, e);
		}

		return added;
	}

	@Override
	public final boolean removeProtocolListener(ProtocolListener<T> listener) {
		Assertion.notNull(listener);

		boolean removed = false;
		try {
			removed = this.protocolListeners.remove(listener);
			if (removed) {
				if (log.isDebugEnabled()) {
					log.debug("listener " + listener.toString() + " removed for protocol " + this.toString());
				}
			} else {
				log.warn("listener " + listener.toString() + " removal faild for protocol " + this.toString());
			}
		} catch (Exception e) {
			log.error("Error removing protocol listener (" + listener + ") from listeners list for " + this, e);
		}

		return removed;
	}

	@Override
	public void negotiate() { }

	public final boolean containsProtocolListener(ProtocolListener<T> listener) {

		boolean rval = false;
		try {
			rval = this.protocolListeners.contains(listener);
		} catch (Exception e) {
			log.error("Error removing protocol listener (" + listener + ") from listeners list for " + this, e);
		}
		return rval;
	}

	protected final void removeProtocolListeners(List<ProtocolListener<T>> listeners) {

		try {
			for (ProtocolListener<T> listener : listeners) {
				try {
					this.protocolListeners.remove(listener);
				} catch (RuntimeException e) {
					log.error("Error removing error prone listener (" + listener + ") for " + this);
				}
			}
		} catch (Exception e) {
			log.error("Error removing error prone listeners from list for " + this, e);
		}
	}

	protected final void copyProtocolListeners(AbstractBroadcastingProtocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					protocol.addProtocolListener(listener);
				} catch (RuntimeException e) {
					log.error("Error adding listener (" + listener + ") to " + protocol);
				}
			}
		} catch (Exception e) {
			log.error("Error adding listener to  " + protocol, e);
		}
	}

	protected final void broadcastOnConnect(ChannelHandler handler) {
		this.broadcastOnConnect(handler, this);
	}

	protected final void broadcastOnConnect(ChannelHandler handler, Protocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					listener.onConnect(handler, protocol);
				} catch (RuntimeException e) {
					log.error(String.format("Error notifying protocol listener of connect -- handler: %s protocol: %s listener %s", handler, protocol, listener), e);
				}
			}
		} catch (Exception e) {
			log.error("Error notifying listeners of connect for " + this);
		}
	}

	protected final void broadcastDataReceived(T data, ChannelHandler handler) {
		this.broadcastDataReceived(data, handler, this);
	}

	protected final void broadcastDataReceived(T data, ChannelHandler handler, Protocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					listener.onDataReceived(data, handler, protocol);
				} catch (RuntimeException e) {
					log.error(String.format("Error notifying protocol listener of data received -- handler: %s protocol: %s listener %s", handler, protocol, listener), e);
				}
			}
		} catch (Exception e) {
			log.error("Error notifying listeners of data for " + this);
		}
	}

	protected final void broadcastDataReceived(List<T> dataList, ChannelHandler handler) {
		this.broadcastDataReceived(dataList, handler, this);
	}

	protected final void broadcastDataReceived(List<T> dataList, ChannelHandler handler, Protocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					for (T data : dataList) {
						listener.onDataReceived(data, handler, protocol);
					}
				} catch (RuntimeException e) {
					log.error(String.format("Error notifying protocol listener of data received -- handler: %s protocol: %s listener %s", handler, protocol, listener), e);
				}
			}
		} catch (Exception e) {
			log.error("Error notifying listeners of data received for " + this);
		}
	}

	protected final void broadcastInboundClose(ChannelHandler handler) {
		this.broadcastInboundClose(handler, this);
	}

	protected final void broadcastInboundClose(ChannelHandler handler, Protocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					listener.onInboundClose(handler, protocol);
				} catch (Exception e) {
					log.error(String.format("Error notifying protocol listener of close -- handler: %s protocol: %s listener %s", handler, protocol, listener), e);
				}
			}
		} catch (Exception e) {
			log.error("Error notifying listeners of close for " + this);
		}
	}

	protected final void broadcastOutboundClose(ChannelHandler handler) {
		this.broadcastOutboundClose(handler, this);
	}

	protected final void broadcastOutboundClose(ChannelHandler handler, Protocol<T> protocol) {

		try {
			for (ProtocolListener<T> listener : protocolListeners) {
				try {
					listener.onOutboundClose(handler, protocol);
				} catch (Exception e) {
					log.error(String.format("Error notifying protocol listener of close -- handler: %s protocol: %s listener %s", handler, protocol, listener), e);
				}
			}
		} catch (Exception e) {
			log.error("Error notifying listeners of close for " + this);
		}
	}
	
	public ConcurrentLinkedQueue<ProtocolListener<T>> getProtocolListeners(){
		return protocolListeners;
	}
}
