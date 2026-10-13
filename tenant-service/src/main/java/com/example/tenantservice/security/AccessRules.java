package com.example.tenantservice.security;

import org.springframework.http.HttpMethod;

import java.util.List;

import static com.example.tenantservice.security.Fe.*;

/** Single source of truth: which permission (FE) each API needs. Same pattern as auth-service. */
public final class AccessRules {

    public record Rule(HttpMethod method, String pattern, Fe fe) {
    }

    public static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/api/v1/tenants", TENANT_CREATE),
            new Rule(HttpMethod.GET, "/api/v1/tenants", TENANT_READ),
            new Rule(HttpMethod.GET, "/api/v1/tenants/{tenantId}", TENANT_READ),
            new Rule(HttpMethod.PATCH, "/api/v1/tenants/{tenantId}", TENANT_UPDATE),
            new Rule(HttpMethod.POST, "/api/v1/tenants/{tenantId}/suspend", TENANT_SUSPEND),
            new Rule(HttpMethod.POST, "/api/v1/tenants/{tenantId}/reactivate", TENANT_REACTIVATE),
            new Rule(HttpMethod.DELETE, "/api/v1/tenants/{tenantId}", TENANT_DEACTIVATE),

            new Rule(HttpMethod.GET, "/api/v1/members", MEMBER_READ),
            new Rule(HttpMethod.GET, "/api/v1/members/{memberId}", MEMBER_READ));

    private AccessRules() {
    }
}
