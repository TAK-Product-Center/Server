package com.bbn.marti.swaggerconfig;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.bbn.marti.config.Docs;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.UnauthorizedException;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.marti.util.CommonUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SwaggerAuthorizationFilter extends OncePerRequestFilter {

	volatile CommonUtil commonUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (adminOnly() && !martiUtil().isAdmin(req)) {
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
        if (commonUtil == null) {
            synchronized(this) {
                if (commonUtil == null) {
                    if (SpringContextBeanForApi.getSpringContext() != null) {
                        commonUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);
                    }
                }
            }
        }
        return commonUtil;
    }
}