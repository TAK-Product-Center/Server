

package com.bbn.marti.nio.codec.impls;

import com.bbn.marti.groups.FileAuthenticator;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.PeriodicUpdateCancellationException;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.codec.impls.AbstractAuthCodec.CodecAuthCallback;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File-based authentication and group assignment ByteCodec
 */
public class FileAuthCodec extends AbstractAuthCodec implements ByteCodec {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public final static ByteCodecFactory codecFactory = new ByteCodecFactory() {

        @Override
        public OrderedExecutor codecExecutor() {
            return null;
        }

        @Override
        public String toString() {
            return "File auth codec factory";
        }

        @Override
        public ByteCodec buildCodec(PipelineContext ctx) {
            return new FileAuthCodec(ctx);
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
                return Lists.newArrayList(getCodecPair());
            }

            @Override
            public String toString() {
                return "File auth codec source - codec list: " + getCodecs();
            }
        };
    }

    public FileAuthCodec(PipelineContext ctx) {
        super(ctx, FileAuthenticator.getInstance());
    }

    /**
     * Methods that are called when each of these events occurs, and all preceeding codecs
     * in the pipeline have already received and passed the event onwards.
     */
    // TODO: Can this be abstracted? Zbasically, can the updateCancelMap be done before or after everything else?
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        // schedule write check, in case we're the initiator
        this.ctx.scheduleWriteCheck();

        // do not proceed without connection information
        if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
            throw new IllegalStateException("connectionId not set in ldap auth codec");
        }

        // set initial cancellation status to false
//        groupUtil.updateCancelMap.put(connectionInfo.getConnectionId(), new AtomicBoolean(false));

        if (logger.isDebugEnabled()) {          	
        	logger.debug("set inital cancellation status for connectionId " + connectionInfo.getConnectionId() + " to false");
        }

        return AsyncFutures.immediateFuture((ByteCodec) this);
    }

    @Override
    public void onInboundClose() {
        onClose();
    }

    @Override
    public void onOutboundClose() {
        onClose();
    }
    private void onClose() {
        // trigger cancellation of periodic auth updates
        if (getConnectionInfo() != null) {
            if (getConnectionInfo().getConnectionId() != null) {
                AtomicBoolean cancelFlag = GroupFederationUtil.getInstance().updateCancelMap.get(getConnectionInfo().getConnectionId());

                if (cancelFlag != null) {
                    cancelFlag.set(true);
                } else {
                    logger.debug("null cancelFlag in in FileAuthCodec onClose()");
                }
            } else {
                logger.debug("null getConnectionInfo().getConnectionId() in FileAuthCodec onClose()");
            }
        } else {
            logger.debug("null connectionInfo in FileAuthCodec onClose()");
        }
    }

    @Override
    protected CodecAuthCallback getNewAuthCallback(String postAuthMessage) {
        return new AbstractAuthCodec.CodecAuthCallback(postAuthMessage) {

            @Override
            public void authenticationReturned(User user, AuthStatus result) {
             
                // set initial cancellation status to false
                if (initialCall.get()) {
                	GroupFederationUtil.getInstance().updateCancelMap.put(connectionInfo.getConnectionId(), new AtomicBoolean(false));

                    logger.debug("set inital cancellation status for connectionId " + connectionInfo.getConnectionId() + " to false");
                } else {
                    
                    // for periodic update case, check for cancellation before proceeding with auth
                    String connectionId = user.getConnectionId();

                    AtomicBoolean cancelFlag = GroupFederationUtil.getInstance().updateCancelMap.get(connectionId);

                    if (cancelFlag == null) {
                        try {
                            groupManager.removeUser(user);
                        } catch (Exception e) {
                            logger.debug("exception removing user " + e.getMessage(), e);
                        }
                        
                        throw new PeriodicUpdateCancellationException("null cancelFlag - cancelling updates for " + user);
                    }

                    if (cancelFlag.get()) {
                        logger.info("cancelling scheduled auth updates for " + user);

                        try {
                            groupManager.removeUser(user);
                        } catch (Exception e) {
                            logger.debug("exception removing user " + e.getMessage(), e);
                        }

                        try {
                        	GroupFederationUtil.getInstance().updateCancelMap.remove(connectionId);
                        } catch (Exception e) {
                            logger.debug("exception clearing updater map for user " + connectionId);
                        }

                        throw new PeriodicUpdateCancellationException("Periodic updates for " + user + " cancelled.");
                    }
                }
    
                // Pass the auth result to the callback defined in AbstractAuthCodec
                super.authenticationReturned(user, result);
            }
        };
    }

    @SuppressWarnings("finally")
    protected void cleanup(ConnectionInfo connectionInfo, User user) {
        try {
            super.cleanup(connectionInfo, user);
        } finally {
            try {
            	GroupFederationUtil.getInstance().updateCancelMap.remove(connectionInfo.getConnectionId());
            } catch (Exception e) {
                logger.debug("exception clearing updater map for " + user);
            }

            throw new PeriodicUpdateCancellationException("Periodic updates for " + user + " cancelled.");
        }
    }
    
    private static Codec getCodecPair() {
        return new Codec(FileAuthCodec.codecFactory, FileAuthCodec.codecFactory);
    }
}
