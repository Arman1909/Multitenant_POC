package com.example.pbm.service;

import com.example.pbm.dto.request.*;
import com.example.pbm.dto.response.*;
import com.example.pbm.entity.*;
import com.example.pbm.repository.*;
import com.example.pbm.security.JwtService;
import com.example.pbm.tenant.TenantContext;
import com.example.pbm.tenant.TenantDirectory;
import com.example.pbm.tenant.TenantInfo;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final TenantDirectory tenants;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TenantContext ctx;

    public LoginResponse login(LoginRequest r) {
        User u = users.findByUsername(r.username())
                .filter(x -> x.isActive() && encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return new LoginResponse(jwt.issue(u, resolveActiveTenant(u, r.tenantId())), "Bearer");
    }

    public LoginResponse switchTenant(SwitchTenantRequest r) {
        User u = users.findById(ctx.userId()).filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Invalid user"));
        if (u.getRole() != Role.TENANT_ADMIN) throw new AccessDeniedException("Only TENANT_ADMIN can switch tenants");
        return new LoginResponse(jwt.issue(u, resolveActiveTenant(u, r.tenantId())), "Bearer");
    }

    /** Token is already validated by the filter chain; this returns the caller's role-specific details. */
    public MeResponse me(Jwt token) {
        User u = users.findById(UUID.fromString(token.getSubject()))
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Invalid user"));
        String t = token.getClaimAsString("tenantId");
        Long activeId = t == null ? null : TenantContext.parseTenantId(t);
        if (u.getRole() != Role.SUPER_ADMIN && (activeId == null || !u.getTenantIds().contains(activeId)))
            throw new AccessDeniedException("Active tenant is not assigned to this user");
        List<TenantInfo> list = u.getRole() == Role.SUPER_ADMIN ? tenants.findAll() : tenants.findAllById(u.getTenantIds());
        TenantResponse active = activeId == null ? null : tenants.find(activeId).map(TenantResponse::from).orElse(null);
        String scope = switch (u.getRole()) {
            case SUPER_ADMIN -> "PLATFORM: list tenants and assign tenant admins. Tenant create/manage is in tenant-service. No access to tenant business data.";
            case TENANT_ADMIN -> "OWN_TENANT: manage users (create/update/deactivate/reactivate) and work with members, groups and claims of the active tenant. Can switch between assigned tenants.";
            case TENANT_USER -> "OWN_TENANT: read users; create/read/update members and groups; read claims of own tenant only.";
        };
        return new MeResponse(true, u.getId(), u.getUsername(), u.getRole(), u.getDeRole(), token.getExpiresAt(),
                new ArrayList<>(u.getRole().fe()), active, list.stream().map(TenantResponse::from).toList(), scope);
    }

    /** The active tenant must be assigned to the user AND be ACTIVE in tenant-service. */
    private Long resolveActiveTenant(User u, Long requested) {
        Long tenant = switch (u.getRole()) {
            case SUPER_ADMIN -> null;
            case TENANT_USER -> {
                if (u.getTenantIds().size() != 1) throw new AccessDeniedException("TENANT_USER must belong to exactly one tenant");
                Long own = u.getTenantIds().iterator().next();
                if (requested != null && !requested.equals(own)) throw new AccessDeniedException("Not assigned to tenant");
                yield own;
            }
            case TENANT_ADMIN -> {
                if (requested == null) throw new IllegalArgumentException("tenantId is required for TENANT_ADMIN");
                if (!u.getTenantIds().contains(requested)) throw new AccessDeniedException("Not assigned to tenant");
                yield requested;
            }
        };
        if (tenant != null) tenants.requireActive(tenant);
        return tenant;
    }
}
