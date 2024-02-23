package tak.server.config.websocket;

import java.security.cert.X509Certificate;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import tak.server.Constants;

/*
 * 
 * Customize the websockets handshake process, so that the Authorization object and session id
 * from the handshake flow are made available in the WebSocket session
 * 
 */
public class SocketAuthHandshakeInterceptor implements HandshakeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SocketAuthHandshakeInterceptor.class);

    public boolean beforeHandshake(ServerHttpRequest request, 
            ServerHttpResponse response, 
            WebSocketHandler wsHandler, 
            Map<String, Object> attributes) 
                    throws Exception {
        
    	if (logger.isDebugEnabled()) {
    		logger.debug("in handshake: " + request + " " + request.getPrincipal() + " " + ((ServletServerHttpRequest) request).getHeaders());

    		logger.debug("http servlet request session: " + ((ServletServerHttpRequest) request).getServletRequest().getSession());
    	}
        
        if (request instanceof ServletServerHttpRequest) {
            
            X509Certificate cert = null;
            
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null) {
                attributes.put(Constants.SOCKET_AUTH_KEY, authentication);
            }
            
            if (session != null) {
                attributes.put(Constants.SOCKET_SESSION_KEY, session.getId());
                session.setAttribute(Constants.SOCKET_SESSION_KEY, session.getId());
                
                try {
                    cert = (X509Certificate) session.getAttribute(Constants.X509_CERT);
                } catch (Exception e) {
                    logger.debug("exception getting X509 cert ", e);
                }
                
                if (cert == null) {
                    throw new IllegalStateException("no client certificate available for socket session");
                }
                
                attributes.put(Constants.X509_CERT, cert);
                
                logger.debug("socket session cert: " + cert);
            }
            
            
            if (PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
                if (((PreAuthenticatedAuthenticationToken) authentication).getCredentials() instanceof X509Certificate) {
                }
            }
        }
        
        return true;
    }

    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception ex) {
    }
}