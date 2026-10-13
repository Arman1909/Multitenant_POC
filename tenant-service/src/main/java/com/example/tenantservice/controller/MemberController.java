package com.example.tenantservice.controller;

import com.example.tenantservice.dto.MemberResponse;
import com.example.tenantservice.security.CurrentCaller;
import com.example.tenantservice.service.MemberQueryService;
import com.example.tenantservice.tenant.TenantContext;
import com.example.tenantservice.tenant.TenantSchemaResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TENANT_ADMIN / TENANT_USER only (MEMBER_READ). The tenant comes from the JWT tenantId claim - not from a
 * header - so a caller can only ever read its own active tenant. X-DE-ROLE still drives the carrier_hq_code RLS
 * filter, because auth-service tokens don't carry DE roles of the DE_<TENANT>_<PREFIX> form yet.
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Members (RLS demo)", description = "carrier_hq_code isolation driven by X-DE-ROLE; tenant from the JWT")
public class MemberController {

    private final TenantSchemaResolver tenantSchemaResolver;
    private final MemberQueryService memberQueryService;

    @Operation(summary = "List members visible to the caller's DE-Role",
            description = "X-DE-ROLE suffix (e.g. MBR/CHA/KHO) filters by carrier_hq_code prefix; _ALL returns every row, enforced by PostgreSQL RLS.")
    @GetMapping
    public List<MemberResponse> listMembers(
            @Parameter(example = "DE_LAKER_MBR")
            @RequestHeader("X-DE-ROLE") String deRole) {
        activateTenant();
        return memberQueryService.listMembers(deRole);
    }

    @Operation(summary = "Get one member; 404 if RLS hides it from this DE-Role")
    @GetMapping("/{memberId}")
    public MemberResponse getMember(
            @PathVariable Long memberId,
            @RequestHeader("X-DE-ROLE") String deRole) {
        activateTenant();
        return memberQueryService.getMember(memberId, deRole);
    }

    private void activateTenant() {
        // TenantContextCleanupFilter clears this after the request
        TenantContext.setCurrentTenant(tenantSchemaResolver.resolveActiveSchema(CurrentCaller.requireActiveTenant()));
    }
}
