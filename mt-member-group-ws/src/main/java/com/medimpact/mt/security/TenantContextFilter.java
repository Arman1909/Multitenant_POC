package com.medimpact.mt.security;

import com.medimpact.mt.common.ApiException;
import com.medimpact.mt.context.CallerContext;
import com.medimpact.mt.context.CallerContextHolder;
import com.medimpact.mt.tenancy.TenantInfo;
import com.medimpact.mt.tenancy.TenantRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after the JWT is validated. Turns the token claims into a {@link CallerContext} and, for OWN_TENANT
 * callers, resolves the active tenant (tenantId claim) to its schema. The tenant ONLY ever comes from the
 * signed token - never from a header, path, query param or body (same rule as auth-service's TenantContext).
 * Not a Spring bean on purpose, so it is not also auto-registered as a plain servlet filter.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantRegistry tenantRegistry;

    public TenantContextFilter(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof Jwt jwt)) {
            chain.doFilter(request, response); // anonymous: authorization rules decide (401/permitAll)
            return;
        }
        try {
            CallerContextHolder.set(buildContext(jwt));
        } catch (ApiException e) {
            writeError(response, request, e);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            CallerContextHolder.clear();
        }
    }

    private CallerContext buildContext(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        String deRole = jwt.getClaimAsString("deRole");
        String tenantClaim = jwt.getClaimAsString("tenantId");
        CallerContext base = new CallerContext(jwt.getSubject(), jwt.getClaimAsString("username"), role, deRole,
                null, null);

        // SUPER_ADMIN / PLATFORM scope never touches tenant data; FE rules return 403 on member/group APIs.
        if (!DeRole.OWN_TENANT.name().equals(deRole) || tenantClaim == null) {
            return base;
        }
        Long tenantId;
        try {
            tenantId = Long.valueOf(tenantClaim);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "JWT tenantId '" + tenantClaim
                    + "' is not a tenant-service tenant_id (expected a number from platform.tenants)");
        }
        TenantInfo tenant = tenantRegistry.resolveActive(tenantId);
        return new CallerContext(base.userId(), base.username(), role, deRole, tenantId, tenant.schemaName());
    }

    private static void writeError(HttpServletResponse response, HttpServletRequest request, ApiException e)
            throws IOException {
        response.setStatus(e.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String detail = e.getMessage().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}"""
                .formatted(e.getStatus().getReasonPhrase(), e.getStatus().value(), detail, request.getRequestURI()));
    }
}
