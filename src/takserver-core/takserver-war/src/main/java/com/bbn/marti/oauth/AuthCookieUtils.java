package com.bbn.marti.oauth;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class AuthCookieUtils {

    public static Cookie createCookie(final String content, final int expirationTimeSeconds) {
        final Cookie cookie = new Cookie(OAuth2AccessToken.ACCESS_TOKEN, content);
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
                String referrer = request.getHeader("referrer");
                response.setHeader("Location", referrer != null ? referrer : "/webtak/index.html");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Pragma", "no-cache");
                return;
            }
        }
    }
}
