

package com.bbn.marti.nio.codec.impls;

import java.io.ByteArrayInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Tls;
import tak.server.federation.DistributedFederationManager;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.util.ByteUtils;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.SSLConfig;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.executor.AsyncExecutor;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;
import com.bbn.roger.fig.FederationUtils;

import com.bbn.marti.remote.config.CoreConfigFacade;

/**
* An implementation of SSL over TCP, implemented at the application layer
*
* Because sun/oracle decided that they coudln't just implement an SSLSocketChannel
*
* Which makes perfect sense
*
* Internally, this codec uses an SSLEngine object (provided by java net) to "wrap" 
* write traffic into ssl, and "unwrap" read traffic from ssl. The ssl engine is very
* sensitive: its needs must constantly be attended to. Like a chia pet or tomagatchi or something. 
* It will only tell you what it wants when you ask it. And you must ask it several times, 
* just to make sure everything is OK.
*
* The bulk of the state diagram is handled in the doRead/doWrite functions: given some
* input, they.
*/
public class SslCodec implements ByteCodec, Comparable<SslCodec> {
    
	private static final Logger logger = LoggerFactory.getLogger(SslCodec.class);
	
    private static final CertificateFactory certFactory;
    
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    
    private final Tls tlsConfig;
    
    static {
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static ByteCodecFactory getSslServerCodecFactory(final Tls tlsConfig) {
        return new ByteCodecFactory() {

            @Override
            public OrderedExecutor codecExecutor() {
                return null;
            }

            @Override
            public String toString() { 
                return "SSL_server_instantiator"; 
            }

            @Override
            public ByteCodec buildCodec(PipelineContext ctx) {
                SslCodec codec = new SslCodec(SSLConfig.getInstance(tlsConfig).buildServerEngine(), ctx, tlsConfig);

                return codec;
            }
        };
    }

	public static ByteCodecFactory getSslClientCodecFactory(final Tls tlsConfig) {
	    return new ByteCodecFactory() {

	        @Override
	        public OrderedExecutor codecExecutor() {
	            return null;
	        }

	        @Override
	        public String toString() { 
	            return "SSL_client_instantiator"; 
	        }

	        @Override
	        public ByteCodec buildCodec(PipelineContext ctx) {
	            SslCodec codec = new SslCodec(SSLConfig.getInstance(tlsConfig).buildClientEngine(), ctx, tlsConfig);

	            return codec;
	        }       
	    };   
	}
    
    public static CodecSource getSslCodecSource(final Tls tlsConfig) {
        return new CodecSource() {
            @Override
            public ByteCodecFactory serverFactory(Codec codec) {
                return getSslServerCodecFactory(tlsConfig);
            }

            @Override
            public ByteCodecFactory clientFactory(Codec codec) {
                return getSslClientCodecFactory(tlsConfig);
            }

            @Override
            public List<Codec> getCodecs() {
                return Lists.newArrayList(SslCodec.getCodecPair(tlsConfig));
            }

            @Override
            public String toString() {
                return "SslCodecSource - codec list: " + getCodecs();
            }
        };
    };
    
    private ConnectionInfo connectionInfo;
    	
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }
    
    public void refreshEngine() {
        log.trace("refreshing SSL engine");
        sslEngine = SSLConfig.getInstance(tlsConfig).getNewSSLEngine(connectionInfo.isClient());
    }

    private final static Logger log = LoggerFactory.getLogger(SslCodec.class);
    private final static int DEFAULT_BUFFER_ALLOC = 2048;
    
    @SuppressWarnings("unused")
    private ChannelHandler handler;

    // pipeline view, for communication
	private final PipelineContext ctx;
	
	// codec we are adapting to
	private SSLEngine sslEngine;
	private ByteBuffer leftovers = null;

    // buffers used as wrap/unwrap destinations
    // held onto because the ssl codec is picky about sizing
    private volatile ByteBuffer inAppBuffer;
    private volatile ByteBuffer outNetBuffer;    
	
	// async object for notifying when the ssl handshake is actually done
	private final SettableAsyncFuture<ByteCodec> handshakeFuture;

    // executor used for processing delegate jobs
    private final AsyncExecutor sslExecutor;

    // job used for scheduling checks on the codec after processing is complete
    private final Runnable writeCheckScheduleJob;

    protected SslCodec(
        SSLEngine engine,
        PipelineContext ctx,
        Tls tlsConfig) 
    {
        this(
            engine,
            ctx,
            AsyncFutures.directSmotheringExecutor(),
            tlsConfig
        );
    }

    protected SslCodec(
        SSLEngine engine,
        PipelineContext ctx,
        AsyncExecutor executor,
        Tls tlsConfig) 
    {
        this.tlsConfig = tlsConfig;
        
        // store context
        this.ctx = ctx;
        
        // allocate buffers
        this.inAppBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_ALLOC);
        
        int appBufferSize = engine.getSession().getPacketBufferSize();
        
        if (logger.isDebugEnabled()) {
        	logger.debug("app buffer size: " + appBufferSize);
        }
        
        this.outNetBuffer = ByteBuffer.allocate(appBufferSize);
        
        // initialize handshake future
        this.handshakeFuture = SettableAsyncFuture.create();
        
        // initialize engine
        this.sslEngine = engine;
        
        // initialize executor -- draw from the static ssl factory, or use a direct
        this.sslExecutor = executor;
        
        // initialize write check job (to be called after handshake processing jobs are complete)
        this.writeCheckScheduleJob = new Runnable() {
            public void run() {
                SslCodec.this.ctx.scheduleWriteCheck();
            }
        };
    }
    
    /**
    * Methods that are called when each of these events occurs, and all preceeding codecs
    * in the pipeline have already received and passed the event onwards.
    */
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        // schedule write check, in case we're the initiator
        
        this.ctx.scheduleWriteCheck();
        
        return this.handshakeFuture;
    }    
    
	/**
	* Receives incoming, network side traffic and decodes it into another byte buffer
	*/
    @Override
	public ByteBuffer decode(ByteBuffer buffer) {
		List<ByteBuffer> reads = doReads(buffer);
		return ByteUtils.concat(reads);
	}
	
	/**
	* Receives outgoing, application side traffic and encodes it into another byte buffer
	*/
    @Override
	public ByteBuffer encode(ByteBuffer buffer) {
//    	if (log.isTraceEnabled()) {
//    		log.trace(this + " processing write");
//    	}

		List<ByteBuffer> writes = doWrites(buffer);
        
        ByteBuffer concat = ByteUtils.concat(writes);
        
//        if (log.isTraceEnabled()) {
//        	log.trace(this + " produced " + concat.remaining() + " bytes of data");
//        }
        
		return concat;
	}

    // TODO: pull all packets out of the engine
    @Override
	public void onInboundClose() {
		try {
			sslEngine.closeInbound();
		
		} catch (SSLException e) {
			log.debug("Received EOS without proper SSL Handshake termination", spelunkToBottomOfExceptionChain(e));
		}
	}
	
    @Override
	public void onOutboundClose() {
		// tell sslEngine that we're closing down
		sslEngine.closeOutbound();
		
		// drain sslEngine of outgoing data, push into pipeline
		List<ByteBuffer> outgoing = doWrites(ByteUtils.getEmptyReadBuffer());

		ctx.scheduleWrite(ByteUtils.concat(outgoing));
	}
    
	private void scheduleReadCheck() {
		this.ctx.scheduleReadCheck();
	}

	private void scheduleWriteCheck() {
        this.ctx.scheduleWriteCheck();
	}
	
	private void setHandshakeFuture() {
    	handshakeFuture.setResult(null);
	}

    /**
    * Submits all delegate tasks to the 
    * executor, returning whether any were 
    * executed at all
    *
    * If there's an error submitting task to the
    * executor, it is forwarded to the surrounding
    * pipeline
    */
	private void drainDelegatedTasks() {
        boolean drainedAny = false;
    
		Runnable task;
		while ((task = sslEngine.getDelegatedTask()) != null) {
			log.trace("Executing background task for ssl engine");
            
			AsyncFuture<Object> handle = null;
            try {
                handle = sslExecutor.submit(task, (Object) null);
                drainedAny = true;
            } catch (RuntimeException e) {
                log.warn("Exception encountered submitting delegate job to ssl executor for execution",
                        spelunkToBottomOfExceptionChain(e));
                ctx.reportException(e);
                return;
            }

            if (handle != null) {
    			handle.addJob(this.writeCheckScheduleJob, sslExecutor);
            }
		}
        
        log.trace("done draining, call a plumber");
        
        if (!drainedAny) {
            scheduleWriteCheck();
        }
	}
	
	private List<ByteBuffer> doReads(ByteBuffer buf) {

	        List<ByteBuffer> reads = new LinkedList<ByteBuffer>();
			ByteBuffer incoming;
			if (leftovers == null) {
				incoming = buf;
			} else {
				incoming = ByteUtils.concat(leftovers, buf);
				leftovers = null;
			}

	        do {
	            ((Buffer)inAppBuffer).clear();

	            SSLEngineResult result = null;

	            try {
	                result = sslEngine.unwrap(incoming, inAppBuffer);
	            } catch (Exception e) {
	                //log.warn("Exception encountered in ssl codec -- read", spelunkToBottomOfExceptionChain(e));
	                Exception root = spelunkToBottomOfExceptionChain(e);
					if (root instanceof javax.crypto.BadPaddingException) {
						leftovers = incoming;
					} else {
						trySetFederateDisabled(e);
						ctx.reportException(root);
					}
					return reads;
	            }

	            Assertion.notNull(result, "SSSLEngine result should be non-null after unwrap");

	            final int consumed = result.bytesConsumed();
	            final int produced = result.bytesProduced();

	            if (log.isTraceEnabled()) {
	            	log.trace(String.format(
	            			"Ssl codec read consumed: %d and produced: %d",
	            			consumed,
	            			produced));
	            }

	            if (produced > 0) {
	                ((Buffer)inAppBuffer).flip();

	                // produced meaningful read data -- add to list to pass towards the application
	                log.trace(this + " decode has remaining bytes: " + inAppBuffer.remaining());

	                ByteBuffer readCopy = ByteUtils.copy(inAppBuffer);
	                reads.add(readCopy);
	            }

	            if (log.isTraceEnabled()) {
	            	log.trace("handshake status: " + result.getHandshakeStatus());
	            }

	            switch (result.getHandshakeStatus()) {
	            case FINISHED:
	            	
	            	if (log.isTraceEnabled()) {
	            		log.trace("Ssl codec finished handshake (read)");
	            	}

	                try {

	                    X509Certificate cert = getPeerCert();

	                    if (cert == null) {
	                        log.warn("peer X509 cerificate not available");
	                    }

	                    if (log.isTraceEnabled()) {
	                    	log.trace("SSLCodec connectionInfo: " + connectionInfo);
	                    }

	                    connectionInfo.setCert(cert);

	                } catch (Exception e) {
	                    log.warn("exception obtaining client cert " + e.getMessage(), e);
	                }  

	                setHandshakeFuture();
	                break;
	            case NOT_HANDSHAKING:

	                break;
	            case NEED_TASK:

	                drainDelegatedTasks();
	                return reads;
	            case NEED_WRAP:
	                log.trace("Needs write check -- scheduling");
	                scheduleWriteCheck();
	                break;
	            case NEED_UNWRAP:
	                if (checkRealloc(result.getStatus(), true, true)) {
	                    // had to reallocate because of overflow -- inAppBuffer wasn't big enough 
	                    // to hold the destination traffic -- try again
	                    continue;
	                }

	                // didn't have to reallocate the inAppBuffer -- need unwrap on more traffic
	                break;
	            default:
	                Assertion.fail("Not implemented");
	            }

	            switch (result.getStatus()) {
	            case BUFFER_OVERFLOW:
	                if (checkRealloc(result.getStatus(), true, false)) {
	                    // in app buffer wasn't large enough -- try again
	                    continue;
	                }

	                // shouldn't ever happen
	                Assertion.fail("Hypothesis: Buffer overflow report should *always* have to reallocate dest buffer");
	                break;
	            case BUFFER_UNDERFLOW:	
	                // didn't have enough data to complete a meaningful read -- return what we have
	                return reads;
	            case CLOSED:
	                // no more read traffic expected -- simply return
	                return reads;
	            case OK:
	                // OK -- keep going around
	                break;
	            default:
	                Assertion.fail();
	            }
	        } while (incoming.hasRemaining());

	        return reads;
	}
	
	// encrypt all available outgoing data, returning the last listenable future given
	private List<ByteBuffer> doWrites(ByteBuffer outgoing) {
		
	        List<ByteBuffer> writes = new LinkedList<ByteBuffer>();
	        
	        do {
	            ((Buffer) outNetBuffer).clear();
	            SSLEngineResult result = null;
	            
	            try {
	                result = sslEngine.wrap(outgoing, outNetBuffer);
	                
	            } catch (Exception e) {

	                trySetFederateDisabled(e);

	                Exception bottomOfExceptionChain = spelunkToBottomOfExceptionChain(e);

	                ctx.reportException(bottomOfExceptionChain);

					if (bottomOfExceptionChain != null && bottomOfExceptionChain instanceof CertificateExpiredException) {

						log.error(String.format("Connection rejected for expired client certificate: %s, %s. Channel: %s Codec: %s",
								getExpiredCertificateName(e), bottomOfExceptionChain.getMessage(), connectionInfo.getHandler(), this));

						((ChannelHandler) connectionInfo.getHandler()).forceClose();
					}

					return writes;
	            }

	            final int consumed = result.bytesConsumed();
	            final int produced = result.bytesProduced();

	            if (produced > 0) {
	                // produced data to go out -- flip for copying, store into write list
	                ((Buffer)outNetBuffer).flip();
	                writes.add(ByteUtils.copy(outNetBuffer));
	            }

	            switch (result.getHandshakeStatus()) {
	            case FINISHED:

	                try {

	                    X509Certificate cert = getPeerCert();

	                    if (cert == null) {
	                        log.warn("peer X509 cerificate not available");
	                    }

	                    connectionInfo.setCert(cert);

	                } catch (Exception e) {
	                    
	                    trySetFederateDisabled(e);
	                    
	                    log.warn("exception obtaining client cert " + e.getMessage(), spelunkToBottomOfExceptionChain(e));
	                }  

	                setHandshakeFuture();
	                break;
	            case NOT_HANDSHAKING:
	                break;
	            case NEED_TASK:
	                drainDelegatedTasks();
	                return writes;
	            case NEED_UNWRAP:
	                scheduleReadCheck();
	                break;
	            case NEED_WRAP:
	                // we just tried to wrap -- check realloc
	                checkRealloc(result.getStatus(), false, true);

	                if (outgoing.hasRemaining()) {
	                    // loop basis is true, we can continue
	                    continue;
	                } else {
	                    // loop basis is false -- if we continue, then 
	                    // we'll break out, even though we want to continue with
	                    // an empty write
	                    writes.addAll(doWrites(outgoing));
	                    break;
	                }
	            default:
	                Assertion.fail();
	            }

	            switch (result.getStatus()) {
	            case BUFFER_OVERFLOW:
	                checkRealloc(result.getStatus(), false, false);

	                if (outgoing.hasRemaining()) {
	                    continue;
	                } else {
	                    writes.addAll(doWrites(outgoing));
	                    break;
	                }
	            case BUFFER_UNDERFLOW:
	                Assertion.fail("Shouldn't ever happen -- any meaningful write should generate");
	            case OK:
	                break;
	            case CLOSED:
	                return writes;
	            default:
	                Assertion.fail();
	            }
	        } while (outgoing.hasRemaining());

	        return writes;
	}

	private SSLSession getSession(boolean inHandshake) {
		SSLSession session;
	
		if (inHandshake) {
			session = sslEngine.getHandshakeSession();
		} else {
			session = sslEngine.getSession();
		}
		
		return session;
	}

	/**
	* A method that checks the status to see if the engine needs to realloc due to overflow/underflow. 
	* The method reallocates in the case of overflow, and returns whether or not any reallocation was 
    * actually done
	*/
	private boolean checkRealloc(
        SSLEngineResult.Status status, // engine status enumeration
        boolean inBound, // indicates whether the resize should target the inAppBuffer or the outNetBuffer;
        boolean inHandshake) // indicates whether we're in a handshake or not
    {
	    
		if (log.isTraceEnabled()) {
			log.trace("checkRealloc - status: " + status + ", inBound: " + inBound + ", inHandshake: " + inHandshake);
		}
	    
		SSLSession session = getSession(inHandshake);
		boolean didRealloc = true; // assume we did realloc
		
		switch (status) {
			case BUFFER_OVERFLOW:
				if (inBound) {
				    
				    if (session == null) {
				        
				    	if (log.isTraceEnabled()) {
				    		log.trace("SSL session is null " + this + " reallocing network -> application buffer to " + DEFAULT_BUFFER_SIZE);
				    	}

				        inAppBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
				    } else {
				    
				        // destination is inApp buffer
				        int buffSize = session.getApplicationBufferSize();
				        if (log.isTraceEnabled()) {
				        	log.trace(this + " reallocing network -> application buffer to: " + buffSize);
				        }
						inAppBuffer = ByteBuffer.allocate(buffSize);
				    }

					
				} else {

					int buffSize = sslEngine.getSession().getPacketBufferSize();

					if (log.isTraceEnabled()) {
						log.trace(this + " reallocing application -> network buffer to: " + buffSize);
					}

					outNetBuffer = ByteBuffer.allocate(buffSize);
				}
				
				break;
			default:
				// do nothing			
				didRealloc = false;
		}
		
		return didRealloc;    
	}
	
    @Override
	public String toString() {
		return "SSL";
	}
    
    private static Codec getCodecPair(final Tls tlsConfig) {
        return new Codec(SslCodec.getSslServerCodecFactory(tlsConfig), SslCodec.getSslClientCodecFactory(tlsConfig));
    }
    
    private X509Certificate getPeerCert() throws SSLPeerUnverifiedException {

        java.security.cert.Certificate[] certs = sslEngine.getSession().getPeerCertificates();

        if (certs == null || certs.length == 0) {
            log.warn("no local client client certs available from SSLEngine");

            return null;
        }

        if (log.isTraceEnabled()) {
        	log.trace("number of peer SSL certs: " + certs.length);
        }

        // pick the first cert

        Certificate cert = certs[0];
        if (certs.length > 1) {
			connectionInfo.setCaCert((X509Certificate) certs[1]);
		}
        // The SSLEngine (always?) produces certs as the "lightly" deprecated  javax.security.cert.X509Certificate type.  Convert these to the newer java.security.cert.X509Certificate type instead.

        try {

            if (cert.getClass().equals(X509Certificate.class)) {
                return (X509Certificate) cert;
            } else {
                
            	if (log.isTraceEnabled()) {
            		log.trace("Client certificate is not a java.security.cert.X509Certificate - type is " + cert.getClass().getName() + " generating new X509Certificate object from cert bytes");
            	}

                Iterator<?> i = certFactory.generateCertificates(new ByteArrayInputStream(cert.getEncoded())).iterator();

                if (i.hasNext()) {
                    return (X509Certificate) i.next();
                }
            }
        } catch (CertificateException e) {
            log.warn("exception processing x509 client cert " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public int compareTo(SslCodec o) {
        return ComparisonChain.start().compare(o.hashCode(), o.hashCode()).result();
    }

	public Exception spelunkToBottomOfExceptionChain(Throwable e) {
	    
		if (e.getCause() != null) {
			return spelunkToBottomOfExceptionChain(e.getCause());
		}
		return (Exception) e;
	}

	public String getExpiredCertificateName(Throwable t) {
		try {
			if (t instanceof CertPathValidatorException) {
				CertPathValidatorException certPathValidatorException = (CertPathValidatorException) t;
				X509Certificate x509Certificate = (X509Certificate) certPathValidatorException
						.getCertPath().getCertificates().get(certPathValidatorException.getIndex());
				return x509Certificate.getSubjectX500Principal().getName();
			} else if (t.getCause() != null) {
				return getExpiredCertificateName(t.getCause());
			}
		} catch (Exception e) {
			log.error("Exception in getExpiredCertificateName!", e);
		}
		return "";
	}

	// try to set federate disabled state if this is a federate connection
	private void trySetFederateDisabled(Exception e) {
	    try {
	        
	        if (connectionInfo == null) {
	            throw new IllegalStateException("connectioInfo not set - not able to check if this is a federate or not");
	        }
	        
	        // detect whether this is a federate based on which TLS config object is used
	        if (tlsConfig.equals(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getTls())) {
	            DistributedFederationManager fedManager = DistributedFederationManager.getInstance();

	            List<FederationOutgoing> outgoings = fedManager.getOutgoingConnections(connectionInfo.getAddress(), connectionInfo.getPort());;
	            
	            if (outgoings.isEmpty()) {
                    throw new TakException("no matching outgoing connection found");
                }

	            FederationOutgoing outgoing = outgoings.get(0);
	            
	            ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederationConnectionStatus(outgoing.getDisplayName());
	            if (status == null) {
	                throw new RuntimeException("no connection status found for this outgoing federate connection");
	            }
	            
	            status.setConnection(connectionInfo);
	            
	            // since there was an exception, there is no cert yet, so this outgoing won't have a federate.
	            	            
	            status.setConnectionStatusValue(ConnectionStatusValue.DISABLED);
	            SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
	            outgoing.setEnabled(false);
	            
	            if (e != null) {
	                Exception eCause = spelunkToBottomOfExceptionChain(e);
	                
	                if (eCause != null) {
	                    status.setLastError(FederationUtils.getHumanReadableErrorMsg(eCause));
	                }
	            }
	            
	        } else {
	            log.debug("non-federate detected - not attempting to set federate disabled state");
	        }

	    } catch (Exception e1) {
	        log.debug("exception setting federate disabled state", e1);
	    }
	}
}
