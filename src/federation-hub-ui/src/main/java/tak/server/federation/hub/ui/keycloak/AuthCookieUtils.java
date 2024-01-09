package tak.server.federation.hub.ui.keycloak;

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

import tak.server.federation.hub.ui.MartiValidator;
import tak.server.federation.hub.ui.MartiValidator.Regex;


public class AuthCookieUtils {

    private static Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(AuthCookieUtils.class);
    private static final int MAX_NAME_VALUE_SIZE = 4096;
   

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict, String path) {
        String validatedName;
        String validatedValue;
        try {
            validatedName = validator.getValidInput(AuthCookieUtils.class.getName(), name,
            		MartiValidator.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, false);
            validatedValue = validator.getValidInput(AuthCookieUtils.class.getName(), value,
            		MartiValidator.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, true);
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

        if (sameSiteStrict) {
            responseCookieBuilder = responseCookieBuilder.sameSite("Strict");
        }

        return responseCookieBuilder.build();
    }

    public static ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict) {
        return createCookie(name, value, maxAge, sameSiteStrict, "/");
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

                response.setHeader("Location", "/");
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
