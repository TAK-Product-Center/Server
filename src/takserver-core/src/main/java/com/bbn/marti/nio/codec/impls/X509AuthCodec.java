

package com.bbn.marti.nio.codec.impls;

import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Input;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.X509Authenticator;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.X509UsernameExtractor;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 *
 * A ByteCodec for X509 authentication and group assignment.
 * 
 */
public class X509AuthCodec extends AbstractAuthCodec implements ByteCodec {

    private static final Logger logger = LoggerFactory.getLogger(X509AuthCodec.class);

    private final X509UsernameExtractor usernameExtractor = new X509UsernameExtractor(DistributedConfiguration.getInstance().getAuth().getDNUsernameExtractorRegex());

    private long lastAuthTime = -1L;
    private long updateIntervalMilliseconds = -1L;
    private final long updateIntervalMillisecondsDefault = 300000L;

    /**
     * Receives incoming, network side traffic and decodes it into another byte buffer
     */
    @Override
    public ByteBuffer decode(ByteBuffer buffer) {

        logger.debug("in X509AuthCodec auth decode");
        
        // do not proceed without connection information
        if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
            throw new IllegalStateException("connectionId not set in AbstractAuthCodec");
        }
        
        doTlsAuth();
        
        return buffer;
    }
    
    /**
     * Receives outgoing, application side traffic and encodes it into another byte buffer
     */
    @Override
    public ByteBuffer encode(ByteBuffer buffer) {
        
        logger.debug("in X509AuthCodec auth encode - auth status: " + authStatus.get());
        
        doTlsAuth();

        return buffer;
    }
    
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        
        // do not proceed without connection information
        if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
            throw new IllegalStateException("connectionId not set in AbstractAuthCodec");
        }
        
        doTlsAuth();
        
        return super.onConnect();
    }
    public X509AuthCodec(PipelineContext ctx) {
        super(ctx, DummyAuthenticator.getInstance());
    }

    @Override
    public void onInboundClose() {
        onClose();
    }

    @Override
    public void onOutboundClose() {
        onClose();
    }
    
    private void onClose() {}

    protected void cleanup(ConnectionInfo connectionInfo, User user) {
        try {
            super.cleanup(connectionInfo, user);
        } finally {
            try {
                GroupFederationUtil.getInstance().updateCancelMap.remove(connectionInfo.getConnectionId());
            } catch (Exception e) {
                logger.debug("exception clearing updater map for " + user);
            }
        }
    }

    public final static ByteCodecFactory getCodecFactory() {

        return new ByteCodecFactory() {

            @Override
            public OrderedExecutor codecExecutor() {
                return null;
            }

            @Override
            public String toString() {
                return "server X509 auth codec factory";
            }

            @Override
            public ByteCodec buildCodec(PipelineContext ctx) {
                X509AuthCodec codec = new X509AuthCodec(ctx);

                return codec;
            }
        };
    };

    public static CodecSource getCodecSource() {
        return new CodecSource() {
            @Override
            public ByteCodecFactory serverFactory(Codec codec) {
                return getCodecFactory();
            }

            @Override
            public ByteCodecFactory clientFactory(Codec codec) {
                return getCodecFactory();
            }

            @Override
            public List<Codec> getCodecs() {
                
                ByteCodecFactory codecFactory = getCodecFactory();
                
                return Lists.newArrayList(new Codec(codecFactory, codecFactory));
            }

            @Override
            public String toString() {
                return "X509 Auth CodecSource - codec list: " + getCodecs();
            }
        };
    }
    
    @Override
    public String toString() {
        return "X509AuthCodec";
    }

    // only do this once
    private synchronized long getUpdateIntervalMilliseconds() {

        if (updateIntervalMilliseconds != -1L) {
            return updateIntervalMilliseconds;
        }

        if (DistributedConfiguration.getInstance() != null &&
            DistributedConfiguration.getInstance().getAuth() != null &&
            DistributedConfiguration.getInstance().getAuth().getLdap() != null &&
            DistributedConfiguration.getInstance().getAuth().getLdap().getUpdateinterval() != null) {
            updateIntervalMilliseconds =
                    DistributedConfiguration.getInstance().getAuth().getLdap().getUpdateinterval() * 1000;
        } else {
            updateIntervalMilliseconds = updateIntervalMillisecondsDefault;
        }

        return updateIntervalMilliseconds;
    }

    protected synchronized void doTlsAuth() {

        // reauthenticate the connection if we've passed the update interval
        long now = (new Date()).getTime();
        if (lastAuthTime + getUpdateIntervalMilliseconds() > now) {
            return;
        }
        lastAuthTime = now;

        logger.debug("doTlsAuth");

        X509Certificate cert = null;
        String username = null;
        try {
            // do not proceed without connection information
            if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
                logger.warn("connectionId not set in AbstractAuthCodec.tryTlsAuth");
                return;
            }

            cert = connectionInfo.getCert();

            if (cert == null) {
                logger.debug("no cert");
                return;
            }

            if (cert.getNotAfter().before(new Date())) {
                throw new TakException("found expired certificate : " + cert.getSubjectDN());
            }

            logger.debug("cert: " + cert);

            if (cert.getSubjectDN() != null) {
                username = usernameExtractor.extractUsername(cert);
            }

            if (Strings.isNullOrEmpty(username)) {
                logger.warn("empty subject name in cert - unable to perform X509 authentication " + cert);
                return;
            }

            User user = new AuthenticatedUser(username, connectionInfo.getConnectionId(), connectionInfo.getAddress(), cert, username, "", ""); // no password or uid

            Input input = null;

            // find the input
            try {
                if (connectionInfo.getHandler() instanceof AbstractBroadcastingChannelHandler) {
                    input = ((AbstractBroadcastingChannelHandler) connectionInfo.getHandler()).getInput();
                }
            } catch (Exception e) { }

            AuthStatus tlsAuthStatus = X509Authenticator.getInstance().authenticate(user, input);

            logger.debug("Core X509 auth status: " + tlsAuthStatus);

            authStatus.set(tlsAuthStatus);

        } catch (Exception e) {
            
            // TODO:
            if (username != null) {
            	
            	logger.error("X509 auth exception info: CN: " + username + ". Message: {}", e.getMessage(), e);

            } else {
            	
                logger.error("X509 auth exception {}", e.getMessage(), e);
                
            }

            if (e instanceof TakException) {
                logger.error("TakException in doTlsAuth {}", e.getMessage(), e);
                if (connectionInfo != null && connectionInfo.getHandler() != null) {
                    ((ChannelHandler) connectionInfo.getHandler()).forceClose();
                }
            }
        }
    }
}
