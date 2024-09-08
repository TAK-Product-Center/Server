package com.bbn.marti.swaggerconfig;

import com.bbn.marti.config.Docs;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.UnauthorizedException;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SwaggerAuthorizationFilter extends OncePerRequestFilter {

	volatile CommonUtil martiUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (adminOnly() && !martiUtil().isAdmin()) {
            throw new UnauthorizedException();
        }

        chain.doFilter(req, res);
    }

    public synchronized boolean adminOnly() {

        if (CoreConfigFacade.getInstance() == null) {
            return true;
        }

        Docs docs = CoreConfigFacade.getInstance().getRemoteConfiguration().getDocs();

        if (docs == null) {
            return true;
        }

        return docs.isAdminOnly();
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