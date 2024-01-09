

package tak.server.federation;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.groups.AbstractAuthenticator;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.codec.impls.AbstractAuthCodec;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.exception.DuplicateFederateException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;

import com.bbn.marti.remote.config.CoreConfigFacade;

/**
 *
 * AuthCodec which handles user and group assignment based on SSL client certs, and Federation config section of CoreConfig, for federated TAKServers.
 *   
 */
public class FederateSslPreAuthCodec extends AbstractAuthCodec implements ByteCodec {
    
    private static final Logger logger = LoggerFactory.getLogger(FederateSslPreAuthCodec.class);

    private final static DistributedFederationManager fedManager = DistributedFederationManager.getInstance();
    
    private final static Federation config = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation();
    
    public final static ByteCodecFactory codecFactory = new ByteCodecFactory() {
    	
        @Override
        public OrderedExecutor codecExecutor() {
            return null;
        }

        @Override
        public String toString() {
            return "Federate SSL auth codec server factory";
        }

        @Override
        public ByteCodec buildCodec(PipelineContext ctx) {
            FederateSslPreAuthCodec codec = new FederateSslPreAuthCodec(ctx, DummyAuthenticator.getInstance());
            return codec;
        }
    };

    public static CodecSource getCodecSource() {
        return new CodecSource() {
            @Override
            public ByteCodecFactory serverFactory(Codec codec) {
                return codecFactory;
            }

            @Override
            public ByteCodecFactory clientFactory(Codec codec) {
                return codecFactory;
            }

            @Override
            public List<Codec> getCodecs() {
                return Lists.newArrayList(FederateSslPreAuthCodec.getCodecPair());
            }

            @Override
            public String toString() {
                return "Federate SSL CodecSource - codec list: " + getCodecs();
            }
        };
    }
    /**
     * Methods that are called when each of these events occurs, and all preceeding codecs
     * in the pipeline have already received and passed the event onwards.
     */
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        // process federate connections sequentially
        synchronized (FederateSslPreAuthCodec.class) {
            // schedule write check, in case we're the initiator
            this.ctx.scheduleWriteCheck();

            handleOnConnect(connectionInfo);

            return AsyncFutures.immediateFuture((ByteCodec) this);
        }
    }
    
	public void handleOnConnect(ConnectionInfo connectionInfo) {
		// do not proceed without connection information
		if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
		    throw new IllegalStateException("connectionId not set in federate SSL auth codec");
		}

		connectionInfo.setTls(true);

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("peer federate connection client cert: " + connectionInfo.getCert() + " connectionInfo " + connectionInfo);
			}

		    if (connectionInfo.getCert() == null) {
		        throw new IllegalStateException("Federate peer client cert not available. Closing connection.");
		    }

		    X509Certificate cert = connectionInfo.getCert();
		    String principalDN = cert.getSubjectX500Principal().getName();
		    String issuerDN = cert.getIssuerX500Principal().getName();

		    String caCertFingerprint = "";
		    if (connectionInfo.getCaCert() != null){
		        caCertFingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(connectionInfo.getCaCert());
		    }
		    //logger.warn("Cert name: " + cn.substring(3, cn.indexOf(',')) + "; issuer name: " + issuer.substring(3, issuer.indexOf(',')));
		    String fingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(cert);

		    // this will throw an exception if the principal or issuer dn can't be obtained
		    String name = MessageConversionUtil.getCN(principalDN) + ":" + MessageConversionUtil.getCN(issuerDN);

		    if (logger.isTraceEnabled()) {
		    	logger.trace("federate user name from DNs: " + name);
		    }

		    // Look for a configured federate with this fingerprint. TODO: possible race condition here for creating / saving these federate configs
		    Federate federate;

		    List<ConnectionStatus> dupeFederates = new ArrayList<>();
		    
		    String dupeMsg = "";

		    // serialize federate config get/set operations
		    federate = fedManager.getFederate(fingerprint);

		    if (federate == null) {
		    	if (logger.isDebugEnabled()) {
		    		logger.debug("CoreConfig federate not found for fingerprint / id: " + fingerprint);
		    	}

		        // put an empty federate with the name and id in the in-memory CoreConfig. Don't save the CoreConfig.xml yet, let that be up to the front-end.
		        federate = new Federate();
		        federate.setId(fingerprint);
		        federate.setName(name);
   
		        for (Federation.FederateCA ca : config.getFederateCA()) {
		            if (ca.getFingerprint().compareTo(caCertFingerprint) == 0) {
		                for (String groupname : ca.getInboundGroup()) {
		                    federate.getInboundGroup().add(groupname);
		                }
		                for (String groupname : ca.getOutboundGroup()) {
		                    federate.getOutboundGroup().add(groupname);
		                }
		                break;
		            }
		        }

		        fedManager.addFederateToConfig(federate);

		        if (logger.isDebugEnabled()) {
		        	logger.debug("federate added to config for id / fingerprint " + fingerprint);
		        }
		    } else {
		    	if (logger.isDebugEnabled()) {
		    		logger.debug("matched existing federate by fingerprint: " + fingerprint + " " + federate.getName());
		    	}
		        
		        // if we matched a federate, there may be an existing connection for it.
		        for (ConnectionStatus status : fedManager.getActiveConnectionInfo()) {
		            if (status.getFederate() != null && status.getFederate().equals(federate)) {
		                
		                dupeFederates.add(status);
		                
		                dupeMsg = "Disallowing duplicate federate connection " + connectionInfo + " for federate " + federate.getName() + " " + federate.getId() + " " + new SecureRandom().nextInt();
		            }
		        }
		    }
		    
		    try {
		        // match this federate with an outgoing connection.
		        if (connectionInfo.isClient()) {
		            logger.info("New Federate connection to: " + connectionInfo.getAddress() + ":"
		                    + connectionInfo.getPort() + "; cert: " + name);

		            if (logger.isTraceEnabled()) {
		            	logger.trace("outgoing federate connection - tracking status");
		            }

		            List<FederationOutgoing> outgoings = fedManager.getOutgoingConnections(connectionInfo.getAddress(), connectionInfo.getPort());

		            if (outgoings.isEmpty()) {
		                throw new RuntimeException("no matching outgoing connection found");
		            }

		            if (!dupeFederates.isEmpty()) {

		                if (!config.isAllowDuplicate()) {
		                    
		                    for (FederationOutgoing outgoing : outgoings) {

		                        fedManager.disableOutgoing(outgoing);
		                    }

		                    throw new DuplicateFederateException(dupeMsg + " " + outgoings.size() + " duplicate outgoing connections found");

		                } else {
		                    logger.warn("allowing duplicate federate connection " + federate.getName() + " " + federate.getId());
		                }
		            }
		            
		            FederationOutgoing outgoing = outgoings.get(0);

		            ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederationConnectionStatus(outgoing.getDisplayName());

		            if (status == null) {
		                throw new RuntimeException("no connection status found for this outgoing federate connection");
		            }

		            status.setConnection(connectionInfo);

		            // put a reference to the matched federate in the connection status
		            status.setFederate(federate);
		            SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(name, status);
		        }
		        else {
		            
		            if (!config.isAllowDuplicate()) {

		                // Attempt to detect a "zombie" federate connection. Meaning a connection that is no longer active, but the socket is still connected for some reason, so no inbound / outbound close event has been detected.
		                if (!dupeFederates.isEmpty()) {
		                    
		                    boolean anyZombies = false;

		                    for (ConnectionStatus dupeStat : dupeFederates) {
		                        if (fedManager.checkAndCloseZombieFederate(dupeStat)) {
		                            anyZombies = true;
		                        }
		                    }

		                    // only throw the dupe exception if zombies where found, to kill
		                    if (!anyZombies) {
		                        throw new DuplicateFederateException(dupeMsg);
		                    }
		                    
		                } else {
		                    logger.debug("possible duplicate federate connection " + federate.getName() + " " + federate.getId());
		                }
		            }

		            logger.info("New Federate connection from: " + connectionInfo.getAddress() + "; cert: " + name);
		        }

		    } catch (DuplicateFederateException e) {
		        throw e; // only let this type of exception propagate
		    } catch (Exception e) {
		        logger.warn("exception setting outgoing connection status " + e.getMessage() , e);
		    }
		    

		    // TODO: add trustchain. Empty for now.
		    FederateUser user = new FederateUser(fingerprint, connectionInfo.getConnectionId(), name, connectionInfo.getAddress(), cert, new X509Certificate[0], federate);
		    groupManager.addUser(user);

		    //Non-Empty inbound or outbound groups, setup user with these groups
		    for (String groupName : federate.getInboundGroup()) {
		        groupManager.addUserToGroup(user, new Group(groupName, Direction.IN));
		    }

		    for (String groupName : federate.getOutboundGroup()) {
		        groupManager.addUserToGroup(user, new Group(groupName, Direction.OUT));
		    }

		} catch (DuplicateFederateException e) {
		    // let this propagate
		    throw e;
		} catch (Exception e) {
		    logger.warn("exception creating federate user: " + e.getMessage(), e);
		}
	}

    public FederateSslPreAuthCodec(PipelineContext ctx, AbstractAuthenticator auth) {
        super(ctx, auth);
    }

    @Override
    public void onInboundClose() {
        processClose();
    }
       
    @Override
    public void onOutboundClose() {
        processClose();
    }
    
    @Override
    public synchronized ByteBuffer decode(ByteBuffer buffer) {
        // pass-thru
        return buffer;   
    }
    
    @Override
    public ByteBuffer encode(ByteBuffer buffer) {
        // pass-thru
        return buffer;
    }
    
    private static Codec getCodecPair() {
        return new Codec(codecFactory, codecFactory);
    }
    
    @Override
    public String toString() {
        return FederateSslPreAuthCodec.class.getSimpleName();
    }
    
    // remove user and groups for this federate
    private void processClose() {     
        // remove federate user
        try {
            User user = groupManager.getUserByConnectionId(connectionInfo.getConnectionId());
            
            if (user != null) {
                groupManager.removeUser(user);
            } else {
            	if (logger.isDebugEnabled()) {
            		logger.debug("null user for " + connectionInfo);
            	}
            }
        
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception removing user for " + connectionInfo);
        	}
        }
        
        try {
            if (connectionInfo.isClient()) {
              
                // only try to set the first outgoing connection that is found
                
                List<FederationOutgoing> outgoings = fedManager.getOutgoingConnections(connectionInfo.getAddress(), connectionInfo.getPort());
                
                if (outgoings.isEmpty()) {
                    throw new TakException("no matching outgoing connection found");
                }
                
                final FederationOutgoing outgoing = outgoings.get(0);
                final ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederationConnectionStatus(outgoing.getDisplayName());

                
                if (status == null) {
                    throw new TakException("no connection status found for this outgoing federate connection");
                }

                fedManager.checkAndSetReconnectStatus(outgoing, "");
            }
        } catch (Exception e) {
            logger.warn("exception setting outgoing connection status " + e.getMessage() , e);
        }
    }
}
