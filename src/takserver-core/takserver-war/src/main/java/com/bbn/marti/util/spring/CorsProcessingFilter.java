package com.bbn.marti.util.spring;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorsProcessingFilter extends OncePerRequestFilter {

    public CorsProcessingFilter() {}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (request.getMethod().matches("OPTIONS") &&
                CorsHeaders.checkAndApplyCorsForConnector(request, response)) {
            return;
        }

        filterChain.doFilter(request, response);
    }
}
