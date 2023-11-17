package tak.server.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.bbn.marti.nio.websockets.BinaryPayloadWebSocketHandler;
import com.bbn.marti.nio.websockets.TakProtoWebSocketHandler;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.Resources;

import tak.server.Constants;
import tak.server.config.websocket.SocketAuthHandshakeInterceptor;

@Configuration
@EnableWebSocket
@Profile({Constants.API_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
public class WebSocketConfiguration implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketConfiguration.class);

	private DistributedConfiguration config = DistributedConfiguration.getInstance();

	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		WebSocketHandlerRegistration takProtoHandler = registry.addHandler(new TakProtoWebSocketHandler(), "/takproto/1");
        
		WebSocketHandlerRegistration binaryPayloadHandler = registry
        		.addHandler(new BinaryPayloadWebSocketHandler(), "/payload/1/*")
        		.addInterceptors(auctionInterceptor());

		if(config.getRemoteConfiguration().getNetwork().isAllowAllOrigins()) {
            takProtoHandler.setAllowedOrigins("*");
            binaryPayloadHandler.setAllowedOrigins("*");
		}
	}
	
    public HandshakeInterceptor auctionInterceptor() {
        return new HandshakeInterceptor() {
			@Override
			public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
					WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

                // Get the URI segment corresponding to the auction id during handshake
                String path = request.getURI().getPath();
                String clientUid = path.substring(path.lastIndexOf('/') + 1);

                // This will be added to the websocket session
                attributes.put("clientUid", clientUid);
                return true;
			}

			@Override
			public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
					WebSocketHandler wsHandler, Exception exception) {}
        };
    }
   
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/cop");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
    	registration.taskExecutor(Resources.websocketExecutor());
        registration.interceptors(sessionContextChannelInterceptorAdapter());
    }
    
    @Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    	registry.setSendBufferSizeLimit(DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketSendBufferSizeLimit());
	}

	@Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
    	registration.taskExecutor(Resources.websocketExecutor());
	}
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketMaxBinaryMessageBufferSize());
        container.setMaxTextMessageBufferSize(DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketMaxBinaryMessageBufferSize());
        
        // negative value for maxSessionIdTimeout will use default
        if (DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketMaxSessionIdleTimeout() > 0) {
        	container.setMaxSessionIdleTimeout(DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketMaxSessionIdleTimeout());
        }
        
        return container;
    }
    
    @Bean
    public ChannelInterceptor sessionContextChannelInterceptorAdapter() {
        
        return new ChannelInterceptor() {
            
            private final Logger logger = LoggerFactory.getLogger(ChannelInterceptor.class);
            
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
            	
                @SuppressWarnings("unused")
                Map<String, Object> sessionHeaders = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
                
                if (logger.isDebugEnabled()) {
                	logger.debug("message headers: " + message.getHeaders());
                }
                
                Map<String, Object> mutableHeaders = new HashMap<String, Object>(message.getHeaders());
                
                if (logger.isDebugEnabled()) {
                	logger.debug("message: " + message + " type: " + message.getClass().getName());
                }
                
                Object payload = message.getPayload();
                
                if (logger.isDebugEnabled()) {
                	logger.debug("message payload: " + payload + " type: " + payload.getClass().getName());
                }
                
                return ChannelInterceptor.super.preSend(message, channel);
            }
        };
    }
    
    public static final class MartiMessageHeaders extends MessageHeaders {

        private static final long serialVersionUID = 9879348571L;

        public MartiMessageHeaders(Map<String, Object> headers) {
            super(headers);
        }
        
        public MartiMessageHeaders(Map<String, Object> headerMap, MessageHeaders messageHeaders) {
            super(headerMap, messageHeaders.getId(), messageHeaders.getTimestamp());
        }
        
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/Marti/api/cop").withSockJS().setInterceptors(new SocketAuthHandshakeInterceptor());
    }
}