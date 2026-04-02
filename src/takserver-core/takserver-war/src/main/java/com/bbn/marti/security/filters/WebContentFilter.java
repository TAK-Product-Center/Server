package com.bbn.marti.security.filters;

import com.bbn.marti.config.AccessFilterType;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.marti.util.CommonUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.NavigableSet;

public class WebContentFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (CoreConfigFacade.getInstance().getRemoteConfiguration().getWebContent() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final List<AccessFilterType> content = CoreConfigFacade.getInstance().getRemoteConfiguration().getWebContent().getAccessFilter();
        final CommonUtil commonUtil = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class);

        for (AccessFilterType filter : content) {
            if (request.getRequestURI().startsWith(filter.getFolder())) {
                if (authentication != null && authentication.isAuthenticated()) {
                    boolean groupMatch = false;

                    if (!filter.getGroup().isEmpty()) {
                        NavigableSet<Group> userGroups = commonUtil.getGroupsFromRequest(request);
                        groupMatch = userGroups.stream().anyMatch(group -> filter.getGroup().contains(group.getName()));
                    }

                    if (!groupMatch) {
                        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
                filterChain.doFilter(request, response); // Continue the filter chain
            }
        }
    }
}
