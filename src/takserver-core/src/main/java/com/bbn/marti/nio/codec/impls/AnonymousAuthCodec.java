

package com.bbn.marti.nio.codec.impls;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Network.Input;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.PeriodicUpdateCancellationException;
import com.bbn.marti.nio.codec.ByteCodec;
import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 *
 * A ByteCodec which, instead of authenticating based on an authentication source such as a file or an LDAP server, assigns every connection an identifier, and puts the resulting user into statically assigned groups, possibly including the designated anonymous group.
 *         
 */
public class AnonymousAuthCodec extends AbstractAuthCodec implements ByteCodec {

    private final List<String> groupList;

    private final boolean anonGroup;
    
    private final boolean isStreaming;
    
    private static final Logger logger = LoggerFactory.getLogger(AnonymousAuthCodec.class);
    
    private final AtomicBoolean initialized = new AtomicBoolean();
   
    /**
     * Receives incoming, network side traffic and decodes it into another byte buffer
     */
    @Override
    public ByteBuffer decode(ByteBuffer buffer) {

        logger.debug("in AnonymousAuthCodec auth decode");
        
        if (initialized.compareAndSet(false, true)) {
            logger.trace("doing init");
            init();
        }
        
        return buffer;
    }
    
    @Override
    public AsyncFuture<ByteCodec> onConnect() {
        
        if (initialized.compareAndSet(false, true)) {
            logger.trace("doing init");
            init();
        }
        
        return super.onConnect();
    }
    public AnonymousAuthCodec(PipelineContext ctx, Input input) {
        super(ctx, DummyAuthenticator.getInstance());

        this.groupList = input.getFiltergroup();
        boolean hasGroups = (groupList != null && !groupList.isEmpty());

        if (input.isAnongroup() == null) {
            this.anonGroup = !hasGroups;
        } else {
            this.anonGroup = input.isAnongroup();
        }

        this.isStreaming = TransportCotEvent.isStreaming(input.getProtocol());
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

    public final static ByteCodecFactory getCodecFactory(final Input input) {

        return new ByteCodecFactory() {

            @Override
            public OrderedExecutor codecExecutor() {
                return null;
            }

            @Override
            public String toString() {
                return "server anonymous auth codec factory";
            }

            @Override
            public ByteCodec buildCodec(PipelineContext ctx) {
                AnonymousAuthCodec codec = new AnonymousAuthCodec(ctx, input);

                return codec;
            }
        };
    };

    public static CodecSource getCodecSource(final Input input) {
        return new CodecSource() {
            @Override
            public ByteCodecFactory serverFactory(Codec codec) {
                return getCodecFactory(input);
            }

            @Override
            public ByteCodecFactory clientFactory(Codec codec) {
                return getCodecFactory(input);
            }

            @Override
            public List<Codec> getCodecs() {
                
                ByteCodecFactory codecFactory = getCodecFactory(input);
                
                return Lists.newArrayList(new Codec(codecFactory, codecFactory));
            }

            @Override
            public String toString() {
                return "AnonymousAuth CodecSource - codec list: " + getCodecs();
            }
        };
    }
    
    private User getAnonymousUser(@Nullable ConnectionInfo connectionInfo) {
        
        if (!isStreaming) {
            return GroupFederationUtil.getInstance().getAnonymousUser();
        }
        
        String username = GroupFederationUtil.ANONYMOUS_USERNAME_BASE + "_" + connectionInfo.getConnectionId();
        
        User user = new AuthenticatedUser(username, connectionInfo.getConnectionId(), connectionInfo.getAddress(), (connectionInfo == null ? null : connectionInfo.getCert()), username, "", "");
        
        return user;
    }
    
    @Override
    public String toString() {
        return "AnonymousAuthCodec";
    }
    
    // create the anonymous users, and do group assignment, for this connection.
    private void init() {
        
        try {

            // do not proceed without connection information
            if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
                throw new IllegalStateException("connectionId not set in anonymous auth codec");
            }

            User user = getAnonymousUser(connectionInfo);

            groupManager.putUserByConnectionId(user, connectionInfo.getConnectionId());

            logger.debug("anonymous user created: " + user);

            Set<String> staticGroups = null;

            if (groupList != null && !groupList.isEmpty()) {
                staticGroups = new HashSet<>(groupList);
            }

            Set<Group> groups = new ConcurrentSkipListSet<>();

            if (anonGroup) {
                logger.debug("enabling default anonymous group");
                groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.IN));
                groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.OUT));
            }

            if (staticGroups != null && !staticGroups.isEmpty()) {
                // configure static groups
                logger.debug("static groups: " + Joiner.on(",").join(staticGroups));

                for(String str : staticGroups) {
                    groups.add(new Group(str, Direction.IN));
                    groups.add(new Group(str, Direction.OUT));
                }
            }

            groupManager.updateGroups(user, groups);
            
            authStatus.set(AuthStatus.SUCCESS);           

        } catch (Exception e) {
            logger.debug("excepting during anonymous onConnect", e);
        }
    }
}
