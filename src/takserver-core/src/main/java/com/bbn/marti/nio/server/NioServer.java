

package com.bbn.marti.nio.server;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.bbn.marti.config.Network;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.selector.AbstractSelectorChange;
import com.bbn.marti.nio.selector.DeregistrationChange;
import com.bbn.marti.nio.selector.KeyAddChange;
import com.bbn.marti.nio.selector.KeyRemoveChange;
import com.bbn.marti.nio.selector.KeySetChange;
import com.bbn.marti.nio.selector.RegistrationChange;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

/**
* A non-blocking IO server implementation for listening on multiple channels/ports/protocols as a server, and sending/receiving on multiple client channels.
* The nio server is a single thread that can be started and stopped. On start, the registered binders (an interface that allows for users
* to specify initial instances of server/client connects) are called, and their channels are registered with the selector. The main thread
* then listens on the selector, which is triggered whenever a registered channel receives a signal, or whenever wakeup is called by another
* thread. This wakeup mechanism is used here to signal the arrival of a selector change request, which can be processed in the absence of 
* any waiting IO events.
*
* Application-side users interact with the server (see Server interface) by submitting registration or interestOp modifications, and by implementing a ChannelHandler.
* These registration/interestOp modifications either register/deregister a channel handler from the server's calls, or change
* the set of calls that a handler can receivecalls. A handler receives a call for each IO event that the server receives from the selector
* for the corresponding channel.
* 
* Any client can submit a selector change request, which returns an asynchronous future containing either the submitted channel handler, upon
* success, or an exception, upon failure.
*
* When an IOEvent is received for a specific channel, the server calls into the handler to notify: the handler should (quickly) process
* the IO event such that the selector will not be retriggered for that particular event, and spin off any tasks necessary to fully process the data
* received. This handler call type returns a flag indicating whether or not the handler should be resubscribed to the IOEvent category it just received. 
*
* The bit-flag format and logic of the InterestOp integer is hidden by means of the typed IOEvent enumeration. Each 
* IOEvent enumeration (READ, WRITE, ACCEPT, CONNECT) corresponds to a single high bit in some integer bit vector, which is tightly coupled
* in the enumeration definition. A subscription to multiple IOEvent is encapsulated with an EnumSet. Conversion to and from the bit vector
* is done on the client thread (in the channel wrapper) to avoid computation on the selector thread while avoiding cumbersome and easily mangled
* bitwise operations.
*
* Client handler and binder writers are responsible for ensuring a few details. All handler implementations should be resilient to spurious calls for IOEvent 
* handling: it cannot be guaranteed that asynchronous calls to unsubscribe or close will be honored before the server calls in.
*
* All channels that are passed to the server, either through a binder or through the channel registration method, should have their channels 
* configured to nonblocking upon instantiation.
*
*/
public class NioServer implements Server, Runnable, Serializable {
	
	private static final long serialVersionUID = -6983711481588072641L;
	
	private static NioServer instance;
	
	public static NioServer getInstance() {
		if (instance == null) {
			synchronized (NioServer.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(NioServer.class);
				}
			}
		}
		
		return instance;
	}

	private final static Logger log = Logger.getLogger(NioServer.class);
    
	private Selector selector = null;		// selector for listening for nio events
	private Thread serverThread = null;		// thread pointer for a current, running server
	private Queue<AbstractSelectorChange> selectorChanges; // queue for storing application-side requests for IO subscription changes until the main thread can process them
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 16); // 2^16

    private final HashMap<String,ChannelWrapper> wrapperMap = new HashMap<>();

	public NioServer() throws IOException {
		this.selectorChanges = new ConcurrentLinkedQueue<AbstractSelectorChange>();

        // try to open the selector
        openSelector();
    }

//	/**
//	* Selector change submission guard: if the nio server is dead (and not processing any 
//	* connections, any change request will immediately throw an exception on submission.
//	*/
//	private boolean serverIsDead() {
//		return (serverThread == null || !serverThread.isAlive() || !selector.isOpen());
//	}

	/**
	* Enqueues the change if the server is awake, or sets an exception if the server is not
	*
	* This method is not enitrely race free with respect to the server being alive/dead, ie, at the moment
	* the server is shut down, it is possible for a submitting thread to enqueue a change that is not 
	* set to except by the server shutdown method (which excepts all pending changes)
	*/
	private boolean wakeupOrExceptChange(AbstractSelectorChange change) {
		if (!selectorChanges.offer(change)) {
			if(log.isWarnEnabled()) {				
				log.warn("Server received change submitted to dead server " + change.toString());
			}
		
			// server is dead or the change was not accepted into the queue
			change.future.setException(new IllegalStateException("Server is not running, or server queue did not accept change"));
			return false;
		} else {
			// wake up the selector to process the change
			this.selector.wakeup();
			return true;
		}
	}

    public AsyncFuture<ChannelHandler> registerChannel(SelectableChannel channel, ChannelHandler handler, IOEvent event) {
        Assertion.areNotNull(channel, handler, event, "None of the arguments passed to the register call can be null");

        RegistrationChange change = new RegistrationChange(SettableAsyncFuture.<ChannelHandler>create(), channel, event.flag(), handler);

        wakeupOrExceptChange(change);

        return change.future;
    }

    public AsyncFuture<ChannelHandler> registerChannel(SelectableChannel channel, ChannelHandler handler, EnumSet<IOEvent> events) {
        Assertion.areNotNull(channel, handler, events, "None of the arguments passed to the register call can be null");

        RegistrationChange change = new RegistrationChange(SettableAsyncFuture.<ChannelHandler>create(), channel, IOEvent.generateFlags(events), handler);

        wakeupOrExceptChange(change);

        return change.future;
    }

    public AsyncFuture<ChannelHandler> deregisterChannel(SelectableChannel channel, ChannelHandler handler) {
        DeregistrationChange change = new DeregistrationChange(SettableAsyncFuture.<ChannelHandler>create(), channel, handler);

        wakeupOrExceptChange(change);

        return change.future;
    }

    public AsyncFuture<ChannelHandler> setInterestOp(SelectableChannel channel, IOEvent event) {
        Assertion.areNotNull(channel, event, "None of the arguments passed to the set interest call can be null");

        KeySetChange change = new KeySetChange(SettableAsyncFuture.<ChannelHandler>create(), channel, event.flag());

        wakeupOrExceptChange(change);

        return change.future;
    }

	public AsyncFuture<ChannelHandler> setInterestOps(SelectableChannel channel, EnumSet<IOEvent> events) {
        Assertion.areNotNull(channel, events, "None of the arguments passed to the set interest call can be null");

        KeySetChange change = new KeySetChange(SettableAsyncFuture.<ChannelHandler>create(), channel, IOEvent.generateFlags(events));

        wakeupOrExceptChange(change);

        return change.future;
	}

    public AsyncFuture<ChannelHandler> addInterestOp(SelectableChannel channel, IOEvent event) {
        Assertion.areNotNull(channel, event, "None of the arguments passed to the add interest call can be null");

        KeyAddChange change = new KeyAddChange(SettableAsyncFuture.<ChannelHandler>create(), channel, event.flag());

        wakeupOrExceptChange(change);

        return change.future;
    }
    public AsyncFuture<ChannelHandler> addInterestOps(SelectableChannel channel, EnumSet<IOEvent> events) {
        Assertion.areNotNull(channel, events, "None of the arguments passed to the add interest call can be null");

        KeyAddChange change = new KeyAddChange(SettableAsyncFuture.<ChannelHandler>create(), channel, IOEvent.generateFlags(events));

        wakeupOrExceptChange(change);

        return change.future;
    }

    public AsyncFuture<ChannelHandler> removeIterestOp(SelectableChannel channel, IOEvent event) {
        Assertion.areNotNull(channel, event, "None of the arguments passed to the remove interest call can be null");

        KeyRemoveChange change = new KeyRemoveChange(SettableAsyncFuture.<ChannelHandler>create(), channel, event.flag());

        wakeupOrExceptChange(change);

        return change.future;
    }
    
    public AsyncFuture<ChannelHandler> removeIterestOps(SelectableChannel channel, EnumSet<IOEvent> events) {
        Assertion.areNotNull(channel, events, "None of the arguments passed to the remove interest call can be null");

        KeyRemoveChange change = new KeyRemoveChange(SettableAsyncFuture.<ChannelHandler>create(), channel, IOEvent.generateFlags(events));

        wakeupOrExceptChange(change);

        return change.future;
    }
		
	/**
	* Method for attempting to open a new selector for this server. 
	*/
	private void openSelector() throws IOException {
        if (this.selector != null && this.selector.isOpen())
            throw new IOException("The selector must be voided out or closed before another is opened");

        try {
    		this.selector = Selector.open();
        } catch (IOException e) {
            throw new IOException(
                "Error opening selector",
                e);
        }        
	}

    /**
     *
     * @param binder
     * @param input Input object
     * @throws IOException
     */
    public void bind(@NotNull ServerBinder binder, @NotNull Network.Input input) throws IOException {
        synchronized (wrapperMap) {
            try {
                ChannelWrapper wrapper = binder.handleBind(this);
                if (wrapper != null) {
                    registerWrapper(binder, wrapper);
                    wrapperMap.put(input.getName(), wrapper);

                    if (wrapper.handler() instanceof AbstractBroadcastingChannelHandler) {
                        AbstractBroadcastingChannelHandler handler = (AbstractBroadcastingChannelHandler) wrapper.handler();
                        handler.withInput(input);
                    }
                    log.info("Successfully bound and registered: " + binder);
                } else {
                    log.info("Network input builder for " + binder + " emitted empty registration during bind -- will be dropped");
                }

            } catch (IOException ioe) {
                // wrap with slightly more informative message, include original throwable cause
                throw new IOException(
                        String.format("IO exception while trying to bind -- binder: %s message: %s", binder, ioe.getMessage()),
                        ioe);
            } catch (Exception e) {
                throw new IOException(
                        String.format("Exception while trying to bind -- binder: %s message: %s", binder, e.getMessage()),
                        e);
            }
        }
    }
    
    /**
    *
    * @param binder
    * @param name
    * @param connectionId Iff set, this will be propagated to client connections as their connectionId
    * @throws IOException
    */
   public void bind(@NotNull ServerBinder binder, @NotNull String name) throws IOException {
       synchronized (wrapperMap) {
           try {
               ChannelWrapper wrapper = binder.handleBind(this);
               if (wrapper != null) {
                   registerWrapper(binder, wrapper);
                   wrapperMap.put(name, wrapper);

                   if (log.isDebugEnabled()) {
                	   log.debug("Successfully bound and registered: " + binder);
                   }
               } else {
                   log.info("Network input builder for " + binder + " emitted empty registration during bind -- will be dropped");
               }

           } catch (IOException ioe) {
               // wrap with slightly more informative message, include original throwable cause
               throw new IOException(
                       String.format("IO exception while trying to bind -- binder: %s message: %s", binder, ioe.getMessage()),
                       ioe);
           } catch (Exception e) {
               throw new IOException(
                       String.format("Exception while trying to bind -- binder: %s message: %s", binder, e.getMessage()),
                       e);
           }
       }
   }

    public void unbind(String inputIdentifier) {
        synchronized (wrapperMap) {
            ChannelWrapper wrapper = wrapperMap.remove(inputIdentifier);
            if (wrapper == null) {
                System.err.println("No input with the identifier \"" + inputIdentifier + "\" found!");
                return;
            }
            SelectableChannel channel = wrapper.channel();
            ChannelHandler handler = wrapper.handler();
            deregisterChannel(channel, handler);

            // TODO: Implement close for other channels
        }
    }

    private void registerWrapper(ServerBinder source, ChannelWrapper wrapper) throws IOException {

        if (serverThread != null && serverThread.isAlive()) {
            // If the server is running, we need to register the new channel asynchronously and wake up the selector.
            // or it may take up to and over 36 seconds to register
            RegistrationChange change = new RegistrationChange(SettableAsyncFuture.<ChannelHandler>create(),
                    wrapper.channel(), wrapper.interest(), wrapper.handler());

            wakeupOrExceptChange(change);
        } else {
            // Otherwise, we can register it normally since the selector will not block it.

            try {
                // register wrapper emitted by handler
                SelectionKey key = wrapper.channel().register(
                        selector,
                        wrapper.interest(),
                        wrapper.handler()
                );
            } catch (IOException ioe) {
                throw new IOException(
                        String.format("IO error while trying to register bound channel with the selector -- binder: %s message: %s", source, ioe.getMessage()),
                        ioe);
            } catch (Exception e) {
                throw new IOException(
                        String.format("Error while trying to register bound channel with the selector -- binder: %s message: %s", source, e.getMessage()),
                        e);
            }
        }
    }

	/**
	* Starts listening and processing incoming traffic. Asserts that no thread
	* is running already, and that the selector is open (ie, bind has been called)
    *
    * @note not thread-safe
	*/
	public void listen() {
		Assertion.condition(this.serverThread == null || !this.serverThread.isAlive(), "Server is already listening");
		Assertion.condition(this.selector != null && this.selector.isOpen(), "Bind has not been called");

		try {
			long mem = Runtime.getRuntime().maxMemory();
			
			log.info("max memory (bytes): " + mem);

		} catch (Throwable t) {
			log.error("exeception getting memory available", t);
		}
		
		log.info("Server started");
		
		this.serverThread = new Thread(this, "NIO Server");
		this.serverThread.start();
	}
    
    /**
    * Interrupts the current thread if it is running, and then (synchronously) joins it.
    */
    public void stop() {
        Assertion.condition(this.serverThread != null && this.serverThread.isAlive(), "Server is not running");
        
        // interrupt thread (which interrupts the selector). Thread itself detects shutdown, checking once per loop cycle
        serverThread.interrupt();
        
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            log.warn("Interrupted while trying to shut down server");
        } finally {
            // void out thread pointer
            serverThread = null;
        }
    }    
    
 	/**
	* Called by the server thread when an interrupt is detected. Iterates over all channels
	* in the selector and closes them, and then closes the selector.
	*/
	private void shutdown() {
		log.info("Server shutting down");

		// close all of the channels registered with the selector				
		closeAllChannels();

		// shut down the actual selector
		closeSelector();
		
		// remove and except all pending selector changes
		closePendingChanges();
	}

    /**
    * Closes all the channels registered with the selector, smothering all exceptions.
    */
    private void closeAllChannels() {
        for (SelectionKey key : selector.keys()) {
            try {
                NetUtils.guardedForceClose(key);
            } catch (RuntimeException e) {
                log.warn("Exception encountered trying to close channel", e);
            }
        }
    }

    /**
    * Method for attempting to close the server's selector only if it is non-null and currently running
    */
    private void closeSelector() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Exception encountered while closing down the selector", e);
        } finally {
            // drop the reference to it
            selector = null;
        }
    }   

	private void closePendingChanges() {
		AbstractSelectorChange change;
		while ((change = selectorChanges.poll()) != null) {
			try {
				change.future.setException(new IllegalStateException("Server shutting down, change not processed"));
			} catch (RuntimeException e) {
                log.warn("Exception excepting pending selector changes", e);
            }
		}
	}

	/**
	* Run forever, until we receive an interrupt (we check the thread status every iteration)
	*/
	public void run() {
        try {
            listenForever();
            log.fatal(this + " exiting server listening loop -- thread was interrupted: " + Thread.currentThread().interrupted());
        } catch (Throwable thrown) {
            log.fatal(this + " exiting server listening loop -- caught unexpected exception", thrown);
            // throw up
            throw thrown;
        }
	}

    private void listenForever() {
        while (true) {
            int selected = -1;
            try {
                // block on waiting for an channel to return -- will return from call with interrupt status set
                // if the thread is interrupted
//                log.trace("Waiting on the selector");
                selected = selector.select(1000);
            } catch (Exception e) {
                log.error("Error calling select on selector", e);
            }

            if (Thread.currentThread().isInterrupted()) {
                // have been asked to stop -- release resources and return
                try {
                    shutdown();
                } catch (Exception e) {
                    log.error("Server encountered exception shutting down", e);
                }
                 
                // jump out
                return;
            }
        
            try {
                // process key set
                processKeySet();
            } catch (RuntimeException e) {
                log.error("Exception encountered processing key set", e);
            }

            try {
               // process selector changes
                processSelectorChanges();
            } catch (RuntimeException e) {
                log.error("Exception encountered processing selector changes", e);
            }
        }
    }

	/**
	* Apply all queued changes to the selector
	*
	* TODO: add limitation into poll mechanism so that nothing terrible happens w.r.t. overload
	*/
	private void processSelectorChanges() {
//        log.trace("processing selector modifications");
    
		AbstractSelectorChange change;
		while ((change = selectorChanges.poll()) != null) {
		    try {
		        change.apply(this.selector);
            } catch (RuntimeException e) {
                log.error("Exception encountered applying selector change", e);
                NetUtils.guardedForceClose(change.channel, selector);
            }
		}
	}

	/**
	* Makes one pass over the channels whose interest keys have been triggered.
	*/
	private void processKeySet() {
//        log.trace("processing key set");
        
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			// remove key from the select key set -- otherwise, it stays in the triggered set
			iter.remove();
			
			if (!key.isValid()) {
				// don't have anything to handle	
				if(log.isTraceEnabled()) {
					log.trace("Server skipping over invalid key");
				}
				continue;
			}
			
			

			final SelectableChannel channel = key.channel();
			final ChannelHandler handler = (ChannelHandler) key.attachment();

			IOEvent event = null;
			boolean staySubscribed = false; // intial assumption of false assures desubscription if an exception is thrown (they get closed anyhow)
			
			try {
				if (key.isReadable()) {
					event = IOEvent.READ;
					staySubscribed = handler.handleRead(channel, this, this.buffer);
				} else if (key.isWritable()) {
					event = IOEvent.WRITE;
					staySubscribed = handler.handleWrite(channel, this, this.buffer);
				} else if (key.isAcceptable()) {
					event = IOEvent.ACCEPT;
					staySubscribed = handler.handleAccept(channel, this);
				} else if (key.isConnectable()) {
					event = IOEvent.CONNECT;
					staySubscribed = handler.handleConnect(channel, this);
				} else {
					// jump out, have nothing to do
					if(log.isWarnEnabled()) {						
						log.warn("Encountered active key with nothing to do");
					}
					continue;
				}
			} catch (RuntimeException e) {
				log.error("Server encountered uncaught exception processing IO event -- force closing the offender", e);
                NetUtils.guardedForceClose(handler);
            }

			if (!staySubscribed && key.isValid()) {
				// unsubscribe the channel from the event they just processed
				try {
				    int newInterest = IOEvent.removeInterest(key.interestOps(), event);
					key.interestOps(newInterest);
				} catch (RuntimeException e) {
					log.error("Exception in main server thread while applying interest modification", e);
                    NetUtils.guardedForceClose(key);
				}
			}
		}
	}

    @Override
    public String toString() {
        return "Nio Server instance (synchronous io strategy)";
    }

    
}


























