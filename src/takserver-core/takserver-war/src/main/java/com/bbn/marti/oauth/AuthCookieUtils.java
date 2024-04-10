package com.bbn.marti.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;


public class AuthCookieUtils {

    private static Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(AuthCookieUtils.class);
    private static final int MAX_NAME_VALUE_SIZE = 4096;

    public enum SameSite {
        Strict, Lax, None
    };

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, SameSite sameSite, String path) {

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
                .secure(true)
                .httpOnly(true)
                .path(path)
                .maxAge(maxAge);

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

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict) {
        SameSite sameSite = SameSite.Lax;
        if (sameSiteStrict) {
            sameSite = SameSite.Strict;
        }
        return createCookie(name, value, maxAge, sameSite, "/");
    }

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, SameSite sameSite) {
        return createCookie(name, value, maxAge, sameSite, "/");
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
            if (cookie.getName().compareToIgnoreCase(OAuth2TokenType.ACCESS_TOKEN.getValue()) == 0) {

                String token = cookie.getValue();

                response.setHeader("Location", "/webtak/index.html");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Pragma", "no-cache");
                response.setHeader(HttpHeaders.SET_COOKIE, createCookie(
                        OAuth2TokenType.ACCESS_TOKEN.getValue(), token, 0, true).toString());

                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                return;
            }
        }
    }
}
