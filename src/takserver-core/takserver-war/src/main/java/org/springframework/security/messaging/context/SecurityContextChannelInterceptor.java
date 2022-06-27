package org.springframework.security.messaging.context;

import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import tak.server.Constants;

/**
 * 
 * 
 * Modifications to this class to fix the method used by Spring to obtain the Authentication, which does not work as advertised.
 * 
 * 
 * <p>
 * Creates a {@link ExecutorChannelInterceptor} that will obtain the
 * {@link Authentication} from the specified {@link Message#getHeaders()}.
 * </p>
 *
 * @since 4.0
 * @author Rob Winch
 */
public final class SecurityContextChannelInterceptor extends ChannelInterceptorAdapter
		implements ExecutorChannelInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityContextChannelInterceptor.class);
    
	private final SecurityContext EMPTY_CONTEXT = SecurityContextHolder
			.createEmptyContext();
	private static final ThreadLocal<Stack<SecurityContext>> ORIGINAL_CONTEXT = new ThreadLocal<Stack<SecurityContext>>();

	private final String authenticationHeaderName;

	private Authentication anonymous = new AnonymousAuthenticationToken("key",
			"anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	/**
	 * Creates a new instance using the header of the name
	 * {@link SimpMessageHeaderAccessor#USER_HEADER}.
	 */
	public SecurityContextChannelInterceptor() {
		this(SimpMessageHeaderAccessor.USER_HEADER);
	}

	/**
	 * Creates a new instance that uses the specified header to obtain the
	 * {@link Authentication}.
	 *
	 * @param authenticationHeaderName the header name to obtain the
	 * {@link Authentication}. Cannot be null.
	 */
	public SecurityContextChannelInterceptor(String authenticationHeaderName) {
		Assert.notNull(authenticationHeaderName,
				"authenticationHeaderName cannot be null");
		this.authenticationHeaderName = authenticationHeaderName;
	}

	/**
	 * Allows setting the Authentication used for anonymous authentication. Default is:
	 *
	 * <pre>
	 * new AnonymousAuthenticationToken(&quot;key&quot;, &quot;anonymous&quot;,
	 * 		AuthorityUtils.createAuthorityList(&quot;ROLE_ANONYMOUS&quot;));
	 * </pre>
	 *
	 * @param authentication the Authentication used for anonymous authentication. Cannot
	 * be null.
	 */
	public void setAnonymousAuthentication(Authentication authentication) {
		Assert.notNull(authentication, "authentication cannot be null");
		this.anonymous = authentication;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		setup(message);
		return message;
	}

	@Override
	public void afterSendCompletion(Message<?> message, MessageChannel channel,
			boolean sent, Exception ex) {
		cleanup();
	}

	public Message<?> beforeHandle(Message<?> message, MessageChannel channel,
			MessageHandler handler) {
		setup(message);
		return message;
	}

	public void afterMessageHandled(Message<?> message, MessageChannel channel,
			MessageHandler handler, Exception ex) {
		cleanup();
	}

	private void setup(Message<?> message) {
		SecurityContext currentContext = SecurityContextHolder.getContext();
		
		logger.debug("sec context message headers: " + message.getHeaders() + " keys: " + message.getHeaders().keySet());
		

		Stack<SecurityContext> contextStack = ORIGINAL_CONTEXT.get();
		if (contextStack == null) {
			contextStack = new Stack<SecurityContext>();
			ORIGINAL_CONTEXT.set(contextStack);
		}
		contextStack.push(currentContext);
		
		Object user = null;
		
		try {
		    
		    @SuppressWarnings("unchecked")
            Map<String, Object> simpAttrs = (Map<String, Object>) message.getHeaders().get("simpSessionAttributes");
		    
		    logger.debug("simpAttrs: " + simpAttrs + " type " + simpAttrs.getClass().getName());
		    
		    if (simpAttrs != null) {
		        Object auth = simpAttrs.get(Constants.SOCKET_AUTH_KEY);
		        logger.debug("auth: " + auth + " type: " + auth.getClass().getName());

		        
		        if (auth instanceof Authentication) {
		            user = auth;
		        }
		        
		    }
		    
		} catch (Exception e) {
		    logger.debug("exception getting marti user: " + e);
		}

		if (user == null) {
		    user = message.getHeaders().get(authenticationHeaderName);
		    
		    logger.debug("sec context using auth from message: " + user);
		}

		Authentication authentication;
		if ((user instanceof Authentication)) {
		    authentication = (Authentication) user;

		    logger.debug("sec context assigned authentication: " + authentication);
		}
		else {
		    authentication = this.anonymous;

		    logger.debug("sec context assigned default anon authentication: " + authentication);
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	private void cleanup() {
		Stack<SecurityContext> contextStack = ORIGINAL_CONTEXT.get();

		if (contextStack == null || contextStack.isEmpty()) {
			SecurityContextHolder.clearContext();
			ORIGINAL_CONTEXT.remove();
			return;
		}

		SecurityContext originalContext = contextStack.pop();

		try {
			if (EMPTY_CONTEXT.equals(originalContext)) {
				SecurityContextHolder.clearContext();
				ORIGINAL_CONTEXT.remove();
			}
			else {
				SecurityContextHolder.setContext(originalContext);
			}
		}
		catch (Throwable t) {
			SecurityContextHolder.clearContext();
		}
	}
}
