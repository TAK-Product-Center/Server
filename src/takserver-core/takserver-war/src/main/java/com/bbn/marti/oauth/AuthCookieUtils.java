package com.bbn.marti.oauth;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;


public class AuthCookieUtils {

    private static Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(AuthCookieUtils.class);
    private static final int MAX_NAME_VALUE_SIZE = 4000;

    public enum SameSite {
        Strict, Lax, None
    };

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, SameSite sameSite, String path, boolean secure) {

        String validatedName;
        String validatedValue;
        try {
            validatedName = validator.getValidInput(AuthCookieUtils.class.getName(), name,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, false);
            validatedValue = validator.getValidInput(AuthCookieUtils.class.getName(), value,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, true);
        } catch (ValidationException e) {
            logger.error("ValidationException in createCookie!", e);
            return null;
        }

        ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie
                .from(validatedName, validatedValue)
                .secure(secure)
                .httpOnly(true)
                .path(path)
                .maxAge(maxAge);

        if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCookie() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getCookie().isCustomDomainEnabled()) {
            String customDomain = CoreConfigFacade.getInstance().getRemoteConfiguration().getCookie().getCustomDomain();
            if (!customDomain.isEmpty()) {
                logger.info("Using custom domain {} for access_token cookie", customDomain);
                responseCookieBuilder = responseCookieBuilder.domain(customDomain);
            } else {
                logger.warn("Custom domain set to empty string, using default domain instead.");
            }
        }

        responseCookieBuilder = responseCookieBuilder.sameSite(sameSite.name());

        return responseCookieBuilder.build();
    }

    /**
     * Gets the string representation of the ResponseCookie and conditionally adds the "Partitioned" attribute.
     * Workaround until Spring Framework supports the Partitioned attribute in ResponseCookie class:
     * https://github.com/spring-projects/spring-framework/issues/31454 (last accessed January 4, 2024)
     * @param responseCookie the ResponseCookie
     * @param partitioned the boolean denoting whether the cookie is partitioned
     * @return a String representation of the <code>cookie</code>, including the "Partitioned" attribute when <code>partitioned</code> is true
     */
    public static String createCookiePartitioned(final ResponseCookie responseCookie, boolean partitioned) {
        String cookie = responseCookie.toString();
        if (!partitioned) return cookie;
        return cookie + (cookie.endsWith(";") ? " " : "; ") + "Partitioned;";
    }

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict, boolean secure) {
        SameSite sameSite = SameSite.Lax;
        if (sameSiteStrict) {
            sameSite = SameSite.Strict;
        }
        return createCookie(name, value, maxAge, sameSite, "/", secure);
    }

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, SameSite sameSite, boolean secure) {
        return createCookie(name, value, maxAge, sameSite, "/", secure);
    }

    public static List<ResponseCookie> createCookiesWithMaxSize(final String name, final String value, int maxAge, boolean sameSiteStrict, boolean secure) {
        List<ResponseCookie> responseCookies = new ArrayList<>();
        List<String> cookieParts = Lists.newArrayList(Splitter.fixedLength(MAX_NAME_VALUE_SIZE).split(value));
        int index = 0;
        for (String cookiePart : cookieParts) {
            responseCookies.add(createCookie(
                    name + "_" + index++, cookiePart,
                    maxAge, sameSiteStrict, secure));
        }
        return responseCookies;
    }

    private static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().compareToIgnoreCase(name) == 0) {
                return cookie;
            }
        }

        return null;
    }

    public static List<Cookie> getAccessTokenCookies(HttpServletRequest request) {
        int index = 0;
        Cookie cookie;
        List<Cookie> cookies = new ArrayList<>();
        while ((cookie = getCookie(
                request, OAuth2TokenType.ACCESS_TOKEN.getValue() + "_" + index++)) != null) {
            cookies.add(cookie);
        }

        return cookies;
    }

    public static String extractAccessTokenFromCookies(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : getAccessTokenCookies(request)) {
            sb.append(cookie.getValue());
        }

        if (sb.length() != 0) {
            return sb.toString();
        }

        return null;
    }

    public static void logout(HttpServletRequest request, HttpServletResponse response) {    	
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(OAuth2TokenType.ACCESS_TOKEN.getValue())) {
                String accessTokenPart = cookie.getValue();
                response.addHeader(HttpHeaders.SET_COOKIE, createCookie(
                        cookie.getName(), accessTokenPart, 0, true, request.isSecure()).toString());
            }
        }

        response.setHeader("Location", "/webtak/index.html");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }

    public static boolean userHasWebtakAccess(Oauth oauthConfig, Claims claims) {
        boolean userHasWebtakAccess = true;
        if (oauthConfig != null
                && oauthConfig.getWebtakScope() != null
                && claims.get(oauthConfig.getScopeClaim()) != null) {
            if (claims.get(oauthConfig.getScopeClaim()) instanceof String) {
                userHasWebtakAccess = ((String) claims.get(oauthConfig.getScopeClaim()))
                        .contains(oauthConfig.getWebtakScope());
            } else {
                userHasWebtakAccess = ((ArrayList<String>) claims.get(oauthConfig.getScopeClaim()))
                        .contains(oauthConfig.getWebtakScope());
            }
        }
        return userHasWebtakAccess;
    }
}
