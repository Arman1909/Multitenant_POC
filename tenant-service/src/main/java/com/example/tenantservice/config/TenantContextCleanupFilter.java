package com.example.tenantservice.config;

import com.example.tenantservice.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Belt-and-braces guarantee: no matter which controller/service code path ran
 * (including future tenant-scoped endpoints, e.g. member APIs, that set
 * TenantContext), the ThreadLocal is always wiped before the worker thread
 * goes back into Tomcat's pool to serve a different, possibly different-tenant,
 * request.
 */
@Component
@Order(Integer.MIN_VALUE)
public class TenantContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
