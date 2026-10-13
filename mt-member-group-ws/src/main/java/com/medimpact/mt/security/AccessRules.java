package com.medimpact.mt.security;

import org.springframework.http.HttpMethod;

import java.util.List;

import static com.medimpact.mt.security.Fe.*;

/**
 * Single source of truth: which permission (FE) each API needs. Same pattern as auth-service.
 * Query-parameter level checks (includeHistory=true needs AUDIT_READ) are done in the controllers via
 * {@link Permissions#require(Fe)} because URL matchers can't see query parameters.
 */
public final class AccessRules {

    public record Rule(HttpMethod method, String pattern, Fe fe) {
    }

    public static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.GET, "/api/v1/members", MEMBER_READ),
            new Rule(HttpMethod.GET, "/api/v1/members/{memberId}", MEMBER_READ),
            new Rule(HttpMethod.GET, "/api/v1/members/{memberId}/groups", MEMBER_READ),
            new Rule(HttpMethod.POST, "/api/v1/members", MEMBER_CREATE),
            new Rule(HttpMethod.PUT, "/api/v1/members/{memberId}", MEMBER_UPDATE),
            new Rule(HttpMethod.DELETE, "/api/v1/members/{memberId}", MEMBER_DEACTIVATE),
            new Rule(HttpMethod.POST, "/api/v1/members/{memberId}/reactivate", MEMBER_REACTIVATE),

            new Rule(HttpMethod.GET, "/api/v1/groups", GROUP_READ),
            new Rule(HttpMethod.GET, "/api/v1/groups/{groupId}", GROUP_READ),
            new Rule(HttpMethod.GET, "/api/v1/groups/{groupId}/members", GROUP_READ),
            new Rule(HttpMethod.POST, "/api/v1/groups", GROUP_CREATE),
            new Rule(HttpMethod.PUT, "/api/v1/groups/{groupId}", GROUP_UPDATE),
            new Rule(HttpMethod.DELETE, "/api/v1/groups/{groupId}", GROUP_DEACTIVATE),
            new Rule(HttpMethod.POST, "/api/v1/groups/{groupId}/reactivate", GROUP_REACTIVATE),
            new Rule(HttpMethod.POST, "/api/v1/groups/{groupId}/members", GROUP_MEMBER_MANAGE),
            new Rule(HttpMethod.DELETE, "/api/v1/groups/{groupId}/members/{memberId}", GROUP_MEMBER_MANAGE));

    private AccessRules() {
    }
}
