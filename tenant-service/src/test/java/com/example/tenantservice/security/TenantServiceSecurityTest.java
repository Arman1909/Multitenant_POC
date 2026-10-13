package com.example.tenantservice.security;

import com.example.tenantservice.controller.MemberController;
import com.example.tenantservice.controller.TenantController;
import com.example.tenantservice.dto.TenantResponse;
import com.example.tenantservice.service.MemberQueryService;
import com.example.tenantservice.service.TenantService;
import com.example.tenantservice.tenant.TenantSchemaResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static com.example.tenantservice.security.TestTokens.bearer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** JWT validation + permission rules. Runs without Docker or a database (services are mocked). */
@WebMvcTest(controllers = {TenantController.class, MemberController.class})
@Import(SecurityConfig.class)
class TenantServiceSecurityTest {

    @Autowired MockMvc mvc;
    @MockBean TenantService tenantService;
    @MockBean TenantSchemaResolver tenantSchemaResolver;
    @MockBean MemberQueryService memberQueryService;

    private static final String CREATE_BODY = "{\"tenantName\":\"Laker\"}";

    private static TenantResponse tenant(long id) {
        return new TenantResponse(id, "Laker", null, "tenant_laker", null, null, null, null, null, null, null,
                "ACTIVE", null, "superadmin", null, "superadmin");
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/tenants")).andExpect(status().isUnauthorized());
    }

    @Test
    void oldTrustedHeaderNoLongerWorks() throws Exception {
        mvc.perform(post("/api/v1/tenants").header("X-USER-ROLE", "SUPER_ADMIN")
                        .contentType("application/json").content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
        verify(tenantService, never()).createTenant(any(), anyString());
    }

    @Test
    void tamperedOrExpiredTokenIs401() throws Exception {
        String t = TestTokens.superAdmin();
        mvc.perform(get("/api/v1/tenants").header("Authorization", bearer(t.substring(0, t.length() - 4) + "AAAA")))
                .andExpect(status().isUnauthorized());
        String expired = TestTokens.token("superadmin", "SUPER_ADMIN", "PLATFORM", null, Duration.ofMinutes(-5));
        mvc.perform(get("/api/v1/tenants").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void superAdminManagesTenantsAndIsAuditedFromToken() throws Exception {
        when(tenantService.listTenants()).thenReturn(List.of(tenant(1)));
        when(tenantService.createTenant(any(), anyString())).thenReturn(tenant(1));
        mvc.perform(get("/api/v1/tenants").header("Authorization", bearer(TestTokens.superAdmin())))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/tenants").header("Authorization", bearer(TestTokens.superAdmin()))
                        .contentType("application/json").content(CREATE_BODY))
                .andExpect(status().isCreated());
        verify(tenantService).createTenant(any(), eq("superadmin")); // entered_by comes from the JWT username
    }

    @Test
    void tenantRolesCannotManageTenants() throws Exception {
        for (String token : List.of(TestTokens.tenantAdmin(1), TestTokens.tenantUser(1))) {
            mvc.perform(get("/api/v1/tenants").header("Authorization", bearer(token))).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/tenants").header("Authorization", bearer(token))
                    .contentType("application/json").content(CREATE_BODY)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/tenants/1/suspend").header("Authorization", bearer(token))).andExpect(status().isForbidden());
        }
        verify(tenantService, never()).createTenant(any(), anyString());
    }

    @Test
    void superAdminGetsNoTenantMemberData() throws Exception {
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(TestTokens.superAdmin()))
                .header("X-DE-ROLE", "DE_LAKER_ALL")).andExpect(status().isForbidden());
    }

    @Test
    void memberReadUsesTenantFromTokenOnly() throws Exception {
        when(tenantSchemaResolver.resolveActiveSchema(7L)).thenReturn("tenant_laker");
        when(memberQueryService.listMembers(anyString())).thenReturn(List.of());
        mvc.perform(get("/api/v1/members").header("Authorization", bearer(TestTokens.tenantUser(7)))
                        .header("X-DE-ROLE", "DE_LAKER_ALL")
                        .header("X-TENANT-ID", "99")) // ignored: tenant comes from the signed token
                .andExpect(status().isOk());
        verify(tenantSchemaResolver).resolveActiveSchema(7L);
        verify(tenantSchemaResolver, never()).resolveActiveSchema(99L);
    }
}
