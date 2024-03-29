package org.springframework.security.web.authentication.preauth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

import com.bbn.marti.remote.exception.NotFoundException;

/**
 * Base class for processing filters that handle pre-authenticated authentication
 * requests, where it is assumed that the principal has already been authenticated by an
 * external system.
 * <p>
 * The purpose is then only to extract the necessary information on the principal from the
 * incoming request, rather than to authenticate them. External authentication systems may
 * provide this information via request data such as headers or cookies which the
 * pre-authentication system can extract. It is assumed that the external system is
 * responsible for the accuracy of the data and preventing the submission of forged
 * values.
 *
 * Subclasses must implement the {@code getPreAuthenticatedPrincipal()} and
 * {@code getPreAuthenticatedCredentials()} methods. Subclasses of this filter are
 * typically used in combination with a {@code PreAuthenticatedAuthenticationProvider},
 * which is used to load additional data for the user. This provider will reject null
 * credentials, so the {@link #getPreAuthenticatedCredentials} method should not return
 * null for a valid principal.
 * <p>
 * If the security context already contains an {@code Authentication} object (either from
 * a invocation of the filter or because of some other authentication mechanism), the
 * filter will do nothing by default. You can force it to check for a change in the
 * principal by setting the {@link #setCheckForPrincipalChanges(boolean)
 * checkForPrincipalChanges} property.
 * <p>
 * By default, the filter chain will proceed when an authentication attempt fails in order
 * to allow other authentication mechanisms to process the request. To reject the
 * credentials immediately, set the
 * {@literal <}tt{@literal >}continueFilterChainOnUnsuccessfulAuthentication{@literal <}/tt{@literal >} flag to false. The exception
 * raised by the {@literal <}tt{@literal >}AuthenticationManager{@literal <}/tt{@literal >} will the be re-thrown. Note that this will
 * not affect cases where the principal returned by {@link #getPreAuthenticatedPrincipal}
 * is null, when the chain will still proceed as normal.
 *
 * @author Luke Taylor
 * @author Ruud Senden
 * @author Rob Winch
 * @since 2.0
 */
public abstract class AbstractPreAuthenticatedProcessingFilter extends GenericFilterBean
		implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher eventPublisher = null;
	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private AuthenticationManager authenticationManager = null;
	private boolean continueFilterChainOnUnsuccessfulAuthentication = true;
	private boolean checkForPrincipalChanges;
	private boolean invalidateSessionOnPrincipalChange = true;
	private AuthenticationSuccessHandler authenticationSuccessHandler = null;
	private AuthenticationFailureHandler authenticationFailureHandler = null;

	/**
	 * Check whether all required properties have been set.
	 */
	@Override
	public void afterPropertiesSet() {
		try {
			super.afterPropertiesSet();
		}
		catch (ServletException e) {
			// convert to RuntimeException for passivity on afterPropertiesSet signature
			throw new RuntimeException(e);
		}
		Assert.notNull(authenticationManager, "An AuthenticationManager must be set");
	}

	/**
	 * Try to authenticate a pre-authenticated user with Spring Security if the user has
	 * not yet been authenticated.
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		if (logger.isDebugEnabled()) {
			logger.debug("Checking secure context token: "
					+ SecurityContextHolder.getContext().getAuthentication());
		}

//		if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//			throw new NotFoundException("username password auth not permitted");
//		}

		if (requiresAuthentication((HttpServletRequest) request)) {
			doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);
		}

		chain.doFilter(request, response);
	}

	/**
	 * Determines if the current principal has changed. The default implementation tries
	 *
	 * <ul>
	 * <li>If the {@link #getPreAuthenticatedPrincipal(HttpServletRequest)} is a String, the {@link Authentication#getName()} is compared against the pre authenticated principal</li>
	 * <li>Otherwise, the {@link #getPreAuthenticatedPrincipal(HttpServletRequest)} is compared against the {@link Authentication#getPrincipal()}
	 * </ul>
	 *
	 * <p>
	 * Subclasses can override this method to determine when a principal has changed.
	 * </p>
	 *
	 * @param request
	 * @param currentAuthentication
	 * @return true if the principal has changed, else false
	 */
	protected boolean principalChanged(HttpServletRequest request, Authentication currentAuthentication) {

		Object principal = getPreAuthenticatedPrincipal(request);

		if ((principal instanceof String) && currentAuthentication.getName().equals(principal)) {
			return false;
		}

		if (principal != null && principal.equals(currentAuthentication.getPrincipal())) {
			return false;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Pre-authenticated principal has changed to " + principal + " and will be reauthenticated");
		}
		return true;
	}

	/**
	 * Do the actual authentication for a pre-authenticated user.
	 */
	private void doAuthenticate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Authentication authResult;

		Object principal = getPreAuthenticatedPrincipal(request);
		Object credentials = getPreAuthenticatedCredentials(request);

		if (principal == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No pre-authenticated principal found in request");
			}

			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("preAuthenticatedPrincipal = " + principal
					+ ", trying to authenticate");
		}

		try {
			PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(
					principal, credentials);
			authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
			authResult = authenticationManager.authenticate(authRequest);
			successfulAuthentication(request, response, authResult);
		}
		catch (AuthenticationException failed) {
			unsuccessfulAuthentication(request, response, failed);

			if (!continueFilterChainOnUnsuccessfulAuthentication) {
				throw failed;
			}
		}
	}

	private boolean requiresAuthentication(HttpServletRequest request) {
		Authentication currentUser = SecurityContextHolder.getContext()
				.getAuthentication();

		if (currentUser == null) {
			return true;
		}

		if (!checkForPrincipalChanges) {
			return false;
		}

		if (!principalChanged(request, currentUser)) {
			return false;
		}

		logger.debug("Pre-authenticated principal has changed and will be reauthenticated");

		if (invalidateSessionOnPrincipalChange) {
			SecurityContextHolder.clearContext();

			HttpSession session = request.getSession(false);

			if (session != null) {
				logger.debug("Invalidating existing session");
				session.invalidate();
				request.getSession();
			}
		}

		return true;
	}

	/**
	 * Puts the <code>Authentication</code> instance returned by the authentication
	 * manager into the secure context.
	 */
	protected void successfulAuthentication(HttpServletRequest request,
			HttpServletResponse response, Authentication authResult) throws IOException, ServletException {
		if (logger.isDebugEnabled()) {
			logger.debug("Authentication success: " + authResult);
		}
		SecurityContextHolder.getContext().setAuthentication(authResult);
		// Fire event
		if (this.eventPublisher != null) {
			eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(
					authResult, this.getClass()));
		}

		if (authenticationSuccessHandler != null) {
			authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
		}
	}

	/**
	 * Ensures the authentication object in the secure context is set to null when
	 * authentication fails.
	 * <p>
	 * Caches the failure exception as a request attribute
	 */
	protected void unsuccessfulAuthentication(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
		SecurityContextHolder.clearContext();

		if (logger.isDebugEnabled()) {
			logger.debug("Cleared security context due to exception", failed);
		}
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, failed);

		if (authenticationFailureHandler != null) {
			authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
		}
	}

	/**
	 * @param anApplicationEventPublisher The ApplicationEventPublisher to use
	 */
	public void setApplicationEventPublisher(
			ApplicationEventPublisher anApplicationEventPublisher) {
		this.eventPublisher = anApplicationEventPublisher;
	}

	/**
	 * @param authenticationDetailsSource The AuthenticationDetailsSource to use
	 */
	public void setAuthenticationDetailsSource(
			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
		Assert.notNull(authenticationDetailsSource,
				"AuthenticationDetailsSource required");
		this.authenticationDetailsSource = authenticationDetailsSource;
	}

	protected AuthenticationDetailsSource<HttpServletRequest, ?> getAuthenticationDetailsSource() {
		return authenticationDetailsSource;
	}

	/**
	 * @param authenticationManager The AuthenticationManager to use
	 */
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	/**
	 * If set to {@code true}, any {@code AuthenticationException} raised by the
	 * {@code AuthenticationManager} will be swallowed, and the request will be allowed to
	 * proceed, potentially using alternative authentication mechanisms. If {@code false}
	 * (the default), authentication failure will result in an immediate exception.
	 *
	 * @param shouldContinue set to {@code true} to allow the request to proceed after a
	 * failed authentication.
	 */
	public void setContinueFilterChainOnUnsuccessfulAuthentication(boolean shouldContinue) {
		continueFilterChainOnUnsuccessfulAuthentication = shouldContinue;
	}

	/**
	 * If set, the pre-authenticated principal will be checked on each request and
	 * compared against the name of the current {@literal <}tt{@literal >}Authentication{@literal <}/tt{@literal >} object. A check to
	 * determine if {@link Authentication#getPrincipal()} is equal to the principal will
	 * also be performed. If a change is detected, the user will be reauthenticated.
	 *
	 * @param checkForPrincipalChanges
	 */
	public void setCheckForPrincipalChanges(boolean checkForPrincipalChanges) {
		this.checkForPrincipalChanges = checkForPrincipalChanges;
	}

	/**
	 * If {@literal <}tt{@literal >}checkForPrincipalChanges{@literal <}/tt{@literal >} is set, and a change of principal is detected,
	 * determines whether any existing session should be invalidated before proceeding to
	 * authenticate the new principal.
	 *
	 * @param invalidateSessionOnPrincipalChange {@literal <}tt{@literal >}false{@literal <}/tt{@literal >} to retain the existing
	 * session. Defaults to {@literal <}tt{@literal >}true{@literal <}/tt{@literal >}.
	 */
	public void setInvalidateSessionOnPrincipalChange(
			boolean invalidateSessionOnPrincipalChange) {
		this.invalidateSessionOnPrincipalChange = invalidateSessionOnPrincipalChange;
	}

	/**
	 * Sets the strategy used to handle a successful authentication.
	 */
	public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
		this.authenticationSuccessHandler = authenticationSuccessHandler;
	}

	/**
	 * Sets the strategy used to handle a failed authentication.
	 */
	public void setAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
		this.authenticationFailureHandler = authenticationFailureHandler;
	}

	/**
	 * Override to extract the principal information from the current request
	 */
	protected abstract Object getPreAuthenticatedPrincipal(HttpServletRequest request);

	/**
	 * Override to extract the credentials (if applicable) from the current request.
	 * Should not return null for a valid principal, though some implementations may
	 * return a dummy value.
	 */
	protected abstract Object getPreAuthenticatedCredentials(HttpServletRequest request);
}
