package com.example.server.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminSecretFilter extends OncePerRequestFilter {

    private static final String ADMIN_HEADER = "X-Admin-Secret";

    private final AdminProperties adminProperties;
    private final AccessDeniedHandler accessDeniedHandler;

    public AdminSecretFilter(
        AdminProperties adminProperties,
        AccessDeniedHandler accessDeniedHandler
    ) {
        this.adminProperties = adminProperties;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresAdminSecret(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret = request.getHeader(ADMIN_HEADER);
        if (!StringUtils.hasText(providedSecret) || !providedSecret.equals(adminProperties.getSecret())) {
            accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("Missing or invalid admin secret")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresAdminSecret(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/admin/")
            && ("POST".equals(request.getMethod())
            || "PUT".equals(request.getMethod())
            || "DELETE".equals(request.getMethod()));
    }
}
