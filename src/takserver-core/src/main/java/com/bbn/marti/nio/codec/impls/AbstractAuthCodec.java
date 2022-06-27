

package com.bbn.marti.nio.codec.impls;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.bbn.cot.exception.AuthBufferExhaustedException;
import com.bbn.cot.exception.AuthenticationFailedException;
import com.bbn.cot.model.AuthCot;
import com.bbn.cot.model.AuthMessage;
import com.bbn.marti.groups.AbstractAuthenticator;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.util.ByteUtils;
import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

/**
 *         
 * Authentication and group assignment ByteCodec base class
 *        
 */
public abstract class AbstractAuthCodec implements ByteCodec {
	
    protected boolean enableLatestSa = DistributedConfiguration.getInstance().getBuffer().getLatestSA().isEnable();

    // auth message buffer size (bytes)
    private static final int BUFFER_SIZE = 1024;

    private final String AUTH_END_MARKER = "</auth>";

    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthCodec.class);

    // buffer to hold potential auth messages
    private ByteBuffer authBuffer;

    private static JAXBContext jaxbContext = null;

    protected final AtomicReference<AuthStatus> authStatus = new AtomicReference<>(AuthStatus.NEW);

    public AtomicReference<AuthStatus> getAuthStatus() {
		return authStatus;
	}

	protected final AbstractAuthenticator authenticator;

    protected final GroupManager groupManager;

    protected ConnectionInfo connectionInfo;

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    static {
        try {
            jaxbContext = JAXBContext.newInstance(AuthMessage.class, AuthCot.class);
        } catch (JAXBException e) {
            logger.warn("exception creating AuthMessage deserialization context", e);
        }
    }
    
    private final AtomicReference<GroupManager> groupManagerRef = new AtomicReference<>();
	
	private GroupManager getGroupManager() {
		if (groupManagerRef.get() == null) {
			synchronized (this) {
				if (groupManagerRef.get() == null) {
					groupManagerRef.set(SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class));
				}
			}
		}
		
		return groupManagerRef.get();
	}

    // pipeline view, for communication
    protected final PipelineContext ctx;

    protected AbstractAuthCodec(PipelineContext ctx, AbstractAuthenticator auth) {  	
        this.ctx = ctx;
        
        groupManager = getGroupManager();

        authBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        authenticator = auth;
        
    }

    /**
     * Methods that are called when each of these events occurs, and all preceeding codecs
     * in the pipeline have already received and passed the event onwards.
     */
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        
        // schedule write check, in case we're the initiator
        this.ctx.scheduleWriteCheck();

        return AsyncFutures.immediateFuture((ByteCodec) this);
    }

    /**
     * Receives incoming, network side traffic and decodes it into another byte buffer
     */
    // Note: this probably doesn't need to be synchronized. Is there any chance that auth message parts could concurrently hit the same authMessage ByteBuffer?
    @Override
    public synchronized ByteBuffer decode(ByteBuffer buffer) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("in AbstractAuthCodec auth decode - auth status: " + authStatus.get());
    	}

        if (authStatus.get().equals(AuthStatus.SUCCESS)) {
            // Only pass data following the auth message for processing in the case of successful authentication
            return buffer;
        }

        // only look for an auth message if auth has not yet been attempted.
        if (authStatus.get().equals(AuthStatus.NEW)) {

            // accumulate this read into the authBuffer
            try {
                authBuffer.put(ByteUtils.getString(buffer).getBytes());
            } catch (java.nio.BufferOverflowException e) {
                throw new AuthBufferExhaustedException("Auth message buffer (" + BUFFER_SIZE + " bytes) exceeded - trigger connection close");
            }

            if (logger.isDebugEnabled()) {
            	logger.debug("remaining: " + authBuffer.remaining());
            }

            int tempSize = BUFFER_SIZE - authBuffer.remaining();
            byte[] authMessageBytes = new byte[tempSize];
            System.arraycopy(authBuffer.array(), 0, authMessageBytes, 0, tempSize);

            String authMessageString = new String(authMessageBytes);

            int pos = authMessageString.toLowerCase().indexOf(AUTH_END_MARKER);

            String authMessageStringOnly = "";
            String remainder = "";

            // store the remainder of this message, to submit once auth has succeeded (if that happens)
            if (pos != -1) {
                try {
                    authMessageStringOnly = authMessageString.substring(0, pos + AUTH_END_MARKER.length());
                    remainder = authMessageString.substring(pos + AUTH_END_MARKER.length(), authMessageString.length());
                    if (logger.isDebugEnabled()) {                    	
                    	logger.debug("remainder data after auth message: " + remainder);
                    }
                } catch (Exception e) {
                    logger.debug("exception getting trimmed auth message", e);
                }
            } else {
                if(authMessageString.contains("<auth>") != true) {
                    doAuth(null, null);
                }
                //logger.debug("end tag not found");
            }

            AuthMessage authMessage = null;

            if (!Strings.isNullOrEmpty(authMessageStringOnly)) {
                try {
                	if (logger.isDebugEnabled()) {                		
                		logger.debug("potential auth message data: " + authMessageStringOnly);
                	}

                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    Document doc = SecureXmlParser.makeDocument(authMessageStringOnly);
                    if(doc != null) {
                        authMessage = (AuthMessage) unmarshaller.unmarshal(doc);
                    }
                } catch (Exception e) {
                    logger.debug("exception deserializing auth message - may be incomplete. message: '" + authMessageString + "'", e);
                }
            }

            if (authMessage != null) {

                authStatus.set(AuthStatus.SUBMITTED);

                doAuth(authMessage, remainder);

                // discard the auth buffer. If auth succeeds, there is no need to process any more auth messages. If it fails, the connection will be closed.
                authBuffer = ByteUtils.getEmptyReadBuffer();

                logger.debug("successfully parsed auth message: " + authMessage);
            }

            // advance the buffer position past the auth message and any other buffered data.
            buffer.position(buffer.position() + buffer.remaining());

            return ByteUtils.getEmptyReadBuffer();
        }

        // fall through to auth result indeterminate state, or auth failure state
        logger.debug(connectionInfo + " current auth status is " + authStatus);

        return ByteUtils.getEmptyReadBuffer();
    }

    /**
     * Receives outgoing, application side traffic and encodes it into another byte buffer
     */
    @Override
    public ByteBuffer encode(ByteBuffer buffer) {
        
    	if (logger.isDebugEnabled()) {
    		logger.debug("in AbstractAuthCodec auth encode - auth status: " + authStatus.get());
    	}
        
        return buffer;
    }

    @Override
    public String toString() {
        return "AbstractAuthCodec";
    }

    // Do authentication and group assignment
    private void doAuth(AuthMessage authMessage, final String postAuthMessage) {

        if (authMessage == null || authMessage.getCot() == null) {
            throw new IllegalStateException("null authMessage or authMessage.getCot()");
        }

        String uid = "";

        if (Strings.isNullOrEmpty(authMessage.getCot().getUsername()) || Strings.isNullOrEmpty(authMessage.getCot().getPassword())) {
            cleanup(connectionInfo, null);
            throw new AuthenticationFailedException("authentication failed - username, password or uid not provided in auth message");
        }

        if (!Strings.isNullOrEmpty(authMessage.getCot().getUid())) {
            uid = authMessage.getCot().getUid();
        }

        final String connectionId = connectionInfo.getConnectionId();

        if (Strings.isNullOrEmpty(connectionId)) {
            throw new IllegalStateException("connectionId is empty. Unable to authenticate.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("AbstractAuthCodec connectionInfo: " + connectionInfo);
        }

        User user = new AuthenticatedUser(authMessage.getCot().getUsername(), connectionId, connectionInfo.getAddress(), connectionInfo.getCert(), authMessage.getCot().getUsername(), authMessage.getCot().getPassword(), uid);

        groupManager.putUserByConnectionId(user, connectionInfo.getConnectionId());
        
        // attempt user authentication, with the given credentials. Also schedule periodic updates for this user.
        authenticator.authenticateAsync(user, getNewAuthCallback(postAuthMessage));
    }

    protected CodecAuthCallback getNewAuthCallback(String postAuthMessage) {
        return new CodecAuthCallback(postAuthMessage);

    }

    protected class CodecAuthCallback implements AuthCallback {

        public CodecAuthCallback(String postAuthmessage) {
            this.postAuthMessage = postAuthmessage;
        }

        // atomic boolean here in case of irrationally small periodic update intervals 
        AtomicBoolean initialCall = new AtomicBoolean(true);
        String postAuthMessage;

        @Override
        synchronized public void authenticationReturned(User user, AuthStatus result) {

            try {
            	
            	if (logger.isTraceEnabled()) {            		
            		logger.trace("in AbstractAuthCodec authenticationReturned - result: " + result + " authStatus: " + authStatus + " user: " + user );
            	}

                switch (result) {
                case SUCCESS:

                    // auth was successful, but no reason to keep this connection if there are no groups for the user
                    if (groupManager.getGroups(user).isEmpty()) {
                        ((ChannelHandler) connectionInfo.getHandler()).forceClose();
                        cleanup(connectionInfo, user);
                        String msg = user + " - is not a member of any groups - disconnecting";
                        logger.info(msg);
                        throw new AuthenticationFailedException(msg);
                    }

                    // Successful authentication 
                    if (initialCall.compareAndSet(true, false)) {
                    	if (logger.isDebugEnabled()) {                    		
                    		logger.debug(user + " authenticated");
                    	}

                        // Set the auth status to success after the post auth message has been sent, to avoid a race condition
                        authStatus.set(AuthStatus.SUCCESS);

                        // note -- this is the "proper" way to do this, the method is there in the PipelineContext, but it's not implemented
                        // ctx.scheduleWrite();

                        // TODO: This really shouldn't be done here and in the SubmissionService. THere should ideally be an additional "onAccessGranted" call or something similar to handle this...
                        // set user on subscription, so that message brokering will be able to find the user
                        
                        DistributedSubscriptionManager subscriptionManager = DistributedSubscriptionManager.getInstance();
                        
                        Subscription subscription = SubscriptionStore.getInstance().getSubscriptionByConnectionInfo(connectionInfo);

                        if (subscription != null) {
                            subscriptionManager.setUserForSubscription(user, subscription);

                            if (enableLatestSa) {
                                // send cached latest sa message for reachable subscriptions from user
                                try {
                                    MessagingUtilImpl.getInstance().sendLatestReachableSA(user);
                                } catch (Exception e) {
                                    logger.error("sendLatestSA threw exception: " + e.getMessage(), e);
                                }
                            }

                            subscriptionManager.startProtocolNegotiation(subscription);
                        }
                        
                        if (logger.isDebugEnabled()) {                          	
                        	logger.debug("subscription in auth " + subscription);
                        }
                    }

                    break;
                case FAILURE:
                    authStatus.set(AuthStatus.FAILURE);
                    // delete the user, in case a previous authenticated attempt succeeded. This will happen in the case of a password change.
                    ((ChannelHandler) connectionInfo.getHandler()).forceClose();
                    cleanup(connectionInfo, user);
                    throw new AuthenticationFailedException("authentication failed for " + user + " - invalid credentials");
                default:
                    authStatus.set(AuthStatus.EXCEPTION);
                    ((ChannelHandler) connectionInfo.getHandler()).forceClose();
                    cleanup(connectionInfo, user);
                    throw new AuthenticationFailedException("authentication failed - unknown authentication status");
                }
                
                if (logger.isTraceEnabled()) {  
                   	logger.trace("AbstractAuthCodec authenticationReturned complete- result: " + result + " authStatus: " + authStatus + " user: " + user );
                }


            } catch (Exception e) {
                logger.error("exception in authenticationReturned : " + e.getMessage(), e);
                throw(e);
            }

        }
    }

    /**
     * Safely disconnects when the user fails to authenticate, logging any exceptions.
     *
     * @param connectionInfo Information relating to the connection
     * @param user           the user to remove; <code>null</code> is allowed.
     */
    protected void cleanup(ConnectionInfo connectionInfo, User user) {

        try {
            if (user != null) {
                groupManager.removeUser(user);
            }
        } catch (Exception ex) {
            logger.error("Problem removing " + user + ": " + ex.getMessage(), ex);
        }

    }
}
