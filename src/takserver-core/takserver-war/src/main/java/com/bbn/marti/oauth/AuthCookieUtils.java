package com.bbn.marti.oauth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;


public class AuthCookieUtils {

    private static Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(AuthCookieUtils.class);
    private static final int MAX_NAME_VALUE_SIZE = 4096;

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict) {

        String validatedName;
        String validatedValue;
        try {
            validatedName = validator.getValidInput(AuthCookieUtils.class.getName(), name,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, false);
            validatedValue = validator.getValidInput(AuthCookieUtils.class.getName(), value,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, false);
        } catch (ValidationException e) {
            logger.error("ValidationException in createCookie!", e);
            return null;
        }

        ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie
                .from(validatedName, validatedValue)
                .secure(true)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge);

        if (sameSiteStrict) {
            responseCookieBuilder = responseCookieBuilder.sameSite("Strict");
        }

        return responseCookieBuilder.build();
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

                response.setHeader("Location", "/webtak/index.html");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Pragma", "no-cache");
                response.setHeader(HttpHeaders.SET_COOKIE, createCookie(
                        OAuth2AccessToken.ACCESS_TOKEN, token, 0, true).toString());

                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                return;
            }
        }
    }
}
