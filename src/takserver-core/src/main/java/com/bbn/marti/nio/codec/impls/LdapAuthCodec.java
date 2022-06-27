

package com.bbn.marti.nio.codec.impls;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.LdapAuthenticator;
import com.bbn.marti.groups.PeriodicUpdateCancellationException;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.google.common.collect.Lists;

/**
 *
 * LDAP authentication and group assignment ByteCodec
 */
public class LdapAuthCodec extends AbstractAuthCodec implements ByteCodec {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapAuthCodec.class);
    
    public final static ByteCodecFactory codecFactory = new ByteCodecFactory() {

        @Override
        public OrderedExecutor codecExecutor() {
            return null;
        }

        @Override
        public String toString() {
            return "LDAP auth codec server factory";
        }

        @Override
        public ByteCodec buildCodec(PipelineContext ctx) {
            LdapAuthCodec codec = new LdapAuthCodec(ctx);

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
                return Lists.newArrayList(LdapAuthCodec.getCodecPair());
            }

            @Override
            public String toString() {
                return "LdapAuthCodecSource - codec list: " + getCodecs();
            }
        };
    }
    
    public LdapAuthCodec(PipelineContext ctx) {
        super(ctx, LdapAuthenticator.getInstance());
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
                    logger.debug("null cancelFlag in in LdapAuthCodec onClose()");
                }
            } else {
                logger.debug("null getConnectionInfo().getConnectionId() in LdapAuthCodec onClose()");
            }
        } else {
            logger.debug("null connectionInfo in LdapAuthCodec onClose()");
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
        return new Codec(codecFactory, codecFactory);
    }
}
