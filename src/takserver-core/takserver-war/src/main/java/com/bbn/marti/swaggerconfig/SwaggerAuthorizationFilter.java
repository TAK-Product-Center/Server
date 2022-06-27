package com.bbn.marti.swaggerconfig;

import com.bbn.marti.config.Docs;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.UnauthorizedException;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/swagger-resources/**", "/swagger-ui.html", "/v2/api-doc"})
public class SwaggerAuthorizationFilter extends OncePerRequestFilter {

    CoreConfig coreConfig;
    CommonUtil martiUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (adminOnly() && !martiUtil().isAdmin()) {
            throw new UnauthorizedException();
        }

        chain.doFilter(req, res);
    }

    public synchronized boolean adminOnly() {

        if (coreConfig() == null) {
            return true;
        }

        Docs docs = coreConfig().getRemoteConfiguration().getDocs();

        if (docs == null) {
            return true;
        }

        return docs.isAdminOnly();
    }

    private CoreConfig coreConfig() {
        if (coreConfig == null) {
            synchronized(this) {
                if (coreConfig == null) {
                    if (SpringContextBeanForApi.getSpringContext() != null) {
                        coreConfig = SpringContextBeanForApi.getSpringContext().getBean(CoreConfig.class);
                    }
                }
            }
        }
        return coreConfig;
    }

    private CommonUtil martiUtil() {
        if (martiUtil == null) {
            synchronized(this) {
                if (martiUtil == null) {
                    if (SpringContextBeanForApi.getSpringContext() != null) {
                        martiUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);
                    }
                }
            }
        }
        return martiUtil;
    }
}