package com.bbn.marti.nio.websockets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Input;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTlsServerHandler;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.TransportCotEvent;

import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.netty.channel.ChannelHandlerContext;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.proto.StreamingProtoBufHelper;


public class NioWebSocketHandler extends NioNettyTlsServerHandler {
    private final static Logger log = LoggerFactory.getLogger(NioWebSocketHandler.class);
    private final String sessionId;
    private final String connectionId;
    private final UUID websocketApiNode;
    private IgniteBiPredicate<UUID, ?> igniteReadListenerPredicate;
    
    private static Input websocketInput = new Input();
   
    static {
    	websocketInput.setProtocol(TransportCotEvent.PROTOTLS.toString());
    }
    
    public NioWebSocketHandler(UUID websocketApiNode, String sessionId, String connectionId, InetSocketAddress local, InetSocketAddress remote) {
        super(websocketInput);
        protobufSupported.set(true);;
        this.websocketApiNode = websocketApiNode;
        this.localSocketAddress = local;
        this.remoteSocketAddress = remote;
        this.sessionId = sessionId;
        this.connectionId = connectionId;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        createConnectionInfo();
        createAuthorizedUser();
        createAdaptedNettyProtocol();
        createAdaptedNettyHandler(connectionInfo);
        ((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyWebsocketWrapper");
        setReader();
        setWriter();
        setNegotiator();
        buildCallbacks();
        createWebsocketSubscription(websocketApiNode);
    }
    
    @Override
    protected void createConnectionInfo() {
        connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId(connectionId);
        connectionInfo.setAddress(remoteSocketAddress.getHostString());
        connectionInfo.setPort(remoteSocketAddress.getPort());
        connectionInfo.setTls(true);
        connectionInfo.setClient(true);
        connectionInfo.setInput(input);
    }
    
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
    	IgniteHolder.getInstance()
    		.getIgnite()
    		.message()
    		.stopLocalListen("websocket-read-listener-" + connectionId, igniteReadListenerPredicate);
       
    	if (channelHandler != null) {
            submissionService.handleChannelDisconnect(channelHandler);
            protocolListeners.forEach(listener -> listener.onOutboundClose(channelHandler, protocol));
        }
    	
		super.channelUnregistered(ctx);

    }
    
    private void setReader() {
    	
        reader = (msg) -> {};
        
        igniteReadListenerPredicate = (nodeId, message) -> {      	
        	ByteBuffer msg = ByteBuffer.wrap((byte[]) message);
        	ByteBuffer leftovers = null;
        	boolean gotMagic = false;
        	int nextSize = 0;
        	int nextShift = 0;
            if (message instanceof byte[]) {
            	try {
        			ByteBuffer fullBuf = null;
        			ByteBuffer buffer = msg;
        			fullBuf = buffer;

        			// try to parse messages out of fullbuf
        			while (fullBuf.remaining() > 0) {

        				// have we read a size from the stream yet?
        				if (nextSize == 0) {

        					// size is preceded by the magic byte
        					if (!gotMagic) {
        						byte nextMagic = fullBuf.get();
        						gotMagic = nextMagic == MAGIC;
        						if (!gotMagic) {
        							log.error("Failed to find magic byte, instead found " + nextMagic);
        							break;
        						}
        					}
        					
        					boolean readSize = false;
        					while (fullBuf.remaining() > 0) {
        						byte b = fullBuf.get();
        						if ((b & 0x80) == 0) {
        							nextSize = nextSize | (b << nextShift);
        							readSize = true;
        							break;
        						} else {
        							nextSize |= (b & 0x7F) << nextShift;
        							nextShift += 7;
        						}
        					}

        					if (!readSize) {
        						// haven't read complete size, stash the fullbuf in leftovers for next time
        						// around
        						leftovers = ByteBuffer.allocate(fullBuf.remaining());
        						leftovers.put(fullBuf);
        						leftovers.flip();
        						break;
        					}
        				}

        				// do we have enough left in the buffer to read out a full message?
        				if (fullBuf.remaining() < nextSize) {
        					// haven't got enough for a message, stash the fullbuf in leftovers for next
        					// time around
        					leftovers = ByteBuffer.allocate(fullBuf.remaining());
        					leftovers.put(fullBuf);
        					leftovers.flip();
        					break;
        				}

        				// copy bytes for next message into eventBytes
        				byte[] eventBytes = new byte[nextSize];
        				fullBuf.get(eventBytes);

        				// parse and broadcast the message
        				TakMessage takMessage = TakMessage.parseFrom(eventBytes);
        				CotEventContainer cotEventContainer = StreamingProtoBufHelper.getInstance().proto2cot(takMessage);

        				if (isNotDOSLimited(cotEventContainer) && isNotReadLimited(cotEventContainer))
        					protocolListeners.forEach(listener -> listener.onDataReceived(cotEventContainer, channelHandler, protocol));

        			}
				} catch (Exception e) {
					log.error("error parsing proto for websocket " + e);
				}
            }
            
            return true;
        };
        
        IgniteHolder.getInstance()
        	.getIgnite()
        	.message(IgniteHolder.getInstance().getIgnite().cluster().forClients().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
        	.localListen("websocket-read-listener-" + connectionId, igniteReadListenerPredicate);
    }
    
    private void setWriter() {

        writer = (data) -> {
            log.info("Websocket Protocol writer is a No-Op, See WebsocketMessagingBroker");
        };
    }
    
    private void setNegotiator() {

        negotiator = () -> {};
    }
    
    private void createAuthorizedUser() {
    	
        try {
            User user = groupManager.getUserByConnectionId(connectionInfo.getConnectionId());

            if (user == null) {
                return;
            }
            
            if (groupManager.getGroups(user).isEmpty()) {
                Set<Group> groups = new ConcurrentSkipListSet<>();
                groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.IN));
                groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.OUT));
                
                groupManager.updateGroups(user, groups);
            }
            
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Error initializing web socket user");
            }
        }
    }
    
    public UUID getApiNode() {
    	return this.websocketApiNode;
    }
}
