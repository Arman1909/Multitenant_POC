package com.example.pbm.tenant;
import com.example.pbm.entity.DeRole;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Only source of tenant/data-scope for services. Never read tenant from request params/body/path. */
@Component
public class TenantContext {
    private Jwt jwt() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof Jwt j) return j;
        throw new AccessDeniedException("Not authenticated");
    }
    public UUID userId() { return UUID.fromString(jwt().getSubject()); }
    public DeRole deRole() { return DeRole.valueOf(jwt().getClaimAsString("deRole")); }
    /** DE check: only OWN_TENANT scope may query tenant data, and it is bound to the active tenant. */
    public Long requireActiveTenant() {
        if (deRole() != DeRole.OWN_TENANT) throw new AccessDeniedException("DE_ROLE does not allow tenant data access");
        return parseTenantId(jwt().getClaimAsString("tenantId"));
    }
    /** tenantId claim = tenant-service tenant_id (BIGINT, sent as a string). */
    public static Long parseTenantId(String claim) {
        if (claim == null) throw new AccessDeniedException("No active tenant");
        try { return Long.valueOf(claim); }
        catch (NumberFormatException e) { throw new AccessDeniedException("Token tenantId is not a platform tenant id; log in again"); }
    }
}
