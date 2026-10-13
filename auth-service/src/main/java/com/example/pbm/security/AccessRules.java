package com.example.pbm.security;

import static com.example.pbm.entity.Fe.*;

import com.example.pbm.entity.Fe;
import java.util.List;
import org.springframework.http.HttpMethod;

/** Single source of truth: which permission (FE) each API needs. */
public final class AccessRules {
    public record Rule(HttpMethod method, String pattern, Fe fe) {}

    public static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.GET,  "/api/v1/tenants", TENANT_READ),
            new Rule(HttpMethod.POST, "/api/v1/tenants/{tenantId}/admins", TENANT_ASSIGN_ADMIN),
            new Rule(HttpMethod.POST, "/api/v1/users", USER_CREATE),
            new Rule(HttpMethod.GET,  "/api/v1/users", USER_READ),
            new Rule(HttpMethod.GET,  "/api/v1/users/{id}", USER_READ),
            new Rule(HttpMethod.PUT,  "/api/v1/users/{id}", USER_UPDATE),
            new Rule(HttpMethod.POST, "/api/v1/users/{id}/deactivate", USER_DEACTIVATE),
            new Rule(HttpMethod.POST, "/api/v1/users/{id}/reactivate", USER_REACTIVATE),
            new Rule(HttpMethod.GET,  "/api/v1/members", MEMBER_READ),
            new Rule(HttpMethod.GET,  "/api/v1/members/{id}", MEMBER_READ),
            new Rule(HttpMethod.GET,  "/api/v1/claims", CLAIM_READ),
            new Rule(HttpMethod.GET,  "/api/v1/claims/{id}", CLAIM_READ));

    private AccessRules() {}
}
