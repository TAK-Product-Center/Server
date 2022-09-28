package com.bbn.marti.oauth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import com.bbn.security.web.MartiValidator;


public class AuthCookieUtils {

    private static Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(AuthCookieUtils.class);

    public static Cookie createCookie(final String content, final int expirationTimeSeconds) {
        String validatedContent;
        try {
            validatedContent = validator.getValidInput(AuthCookieUtils.class.getName(), content,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, false);
        } catch (ValidationException e) {
            logger.error("ValidationException in createCookie!", e);
            return null;
        }

        final Cookie cookie = new Cookie(OAuth2AccessToken.ACCESS_TOKEN, validatedContent);
        cookie.setMaxAge(expirationTimeSeconds);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        return cookie;
    }

    public static void logout(HttpServletRequest request, HttpServletResponse response, DefaultTokenServices defaultTokenServices) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().compareToIgnoreCase(OAuth2AccessToken.ACCESS_TOKEN) == 0) {

                String token = cookie.getValue();

                if (defaultTokenServices != null) {
                    defaultTokenServices.revokeToken(token);
                }

                final Cookie copyCookie = createCookie(token, 0);
                response.addCookie(copyCookie);
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", "/webtak/index.html");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Pragma", "no-cache");
                return;
            }
        }
    }
}
