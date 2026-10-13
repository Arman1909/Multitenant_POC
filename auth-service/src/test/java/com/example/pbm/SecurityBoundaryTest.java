package com.example.pbm;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.pbm.entity.*;
import com.example.pbm.repository.*;
import com.example.pbm.security.JwtService;
import com.example.pbm.service.UserService;
import com.example.pbm.tenant.TenantDirectory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest @AutoConfigureMockMvc
class SecurityBoundaryTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper om;
    @Autowired TenantDirectory tenants; @Autowired UserRepository users; @Autowired JdbcTemplate jdbc;
    @Autowired MemberRepository members; @Autowired ClaimRepository claims; @Autowired JwtService jwtService;
    @Autowired UserService userService;
    Long A, B;
    @BeforeEach void ids() { A = tenants.findByName("Tenant A").get().tenantId(); B = tenants.findByName("Tenant B").get().tenantId(); }

    // Helper methods below are named get/post/put, which shadow the static MockMvcRequestBuilders imports,
    // so the request builders are always called fully qualified.
    String login(String user, Long tenant) throws Exception {
        return om.readTree(loginAttempt(user, "password", tenant).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    ResultActions loginAttempt(String user, String password, Long tenant) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("username", user, "password", password));
        if (tenant != null) body.put("tenantId", tenant);
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }
    ResultActions get(String token, String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }
    ResultActions post(String token, String url, Object body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }
    ResultActions put(String token, String url, Object body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }
    String sfx() { return UUID.randomUUID().toString().substring(0, 8); }

    /** Simulates tenant-service creating a tenant (auth-service only reads platform.tenants). */
    Long newTenant(String status) {
        String name = "T-" + sfx();
        jdbc.update("insert into platform.tenants (tenant_name, schema_name, status) values (?, ?, ?)",
                name, "tenant_" + name.substring(2).replace('-', '_'), status);
        return tenants.findByName(name).get().tenantId();
    }
    /** SUPER_ADMIN assigns a new TENANT_ADMIN (password "password") to a tenant; returns the username. */
    String newAdmin(Long tenant) throws Exception {
        String name = "adm-" + sfx();
        post(login("superadmin", null), "/api/v1/tenants/" + tenant + "/admins", Map.of("username", name, "password", "password"))
            .andExpect(status().isOk());
        return name;
    }
    void assign(String admin, Long tenant) throws Exception {
        post(login("superadmin", null), "/api/v1/tenants/" + tenant + "/admins", Map.of("username", admin)).andExpect(status().isOk());
    }
    UUID idOf(String username) { return users.findByUsername(username).get().getId(); }

    @Test void t01_superAdminLogin() throws Exception { assertFalse(login("superadmin", null).isBlank()); }

    @Test void t02_t03_tenantsComeFromTenantServiceAndAdminCanBeAssigned() throws Exception {
        String s = login("superadmin", null);
        post(s, "/api/v1/tenants", Map.of("name", "T-" + sfx())).andExpect(status().isForbidden()); // create moved to tenant-service
        Long t = newTenant("ACTIVE");
        get(s, "/api/v1/tenants").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(t.intValue())));
        newAdmin(t);
    }

    @Test void t04_adminBelongsToMultipleTenants() {
        assertEquals(Set.of(A, B), users.findByUsername("tenantadmin1").get().getTenantIds());
    }

    @Test void t05_adminSelectsAndSwitchesAssignedTenant() throws Exception {
        String t = login("tenantadmin1", A);
        post(t, "/api/v1/auth/switch-tenant", Map.of("tenantId", B)).andExpect(status().isOk());
    }

    @Test void t06_adminCannotSelectUnassignedTenant() throws Exception {
        Long other = newTenant("ACTIVE");
        loginAttempt("tenantadmin1", "password", other).andExpect(status().isForbidden());
        post(login("tenantadmin1", A), "/api/v1/auth/switch-tenant", Map.of("tenantId", other)).andExpect(status().isForbidden());
    }

    @Test void t07_tenantUserHasExactlyOneTenant() throws Exception {
        assertEquals(1, users.findByUsername("usera1").get().getTenantIds().size());
        loginAttempt("usera1", "password", B).andExpect(status().isForbidden());
        post(login("superadmin", null), "/api/v1/tenants/" + B + "/admins", Map.of("username", "usera1")).andExpect(status().isConflict());
    }

    @Test void t08_t19_adminCreatesUserInActiveTenant_ignoringBodyTenantId() throws Exception {
        String ta = login("tenantadmin1", A); String name = "u-" + sfx();
        post(ta, "/api/v1/users", Map.of("username", name, "password", "password123", "tenantId", B)).andExpect(status().isCreated());
        assertEquals(Set.of(A), users.findByUsername(name).get().getTenantIds());
        get(ta, "/api/v1/users").andExpect(jsonPath("$[*].username", hasItem(name)));
        get(login("tenantadmin1", B), "/api/v1/users").andExpect(jsonPath("$[*].username", not(hasItem(name))));
    }

    @Test void t09_t10_t11_userIsolation() throws Exception {
        UUID userB = idOf("userb1");
        for (String token : List.of(login("tenantadmin1", A), login("usera1", null))) {
            get(token, "/api/v1/users").andExpect(jsonPath("$[*].username", hasItem("usera1"))).andExpect(jsonPath("$[*].username", not(hasItem("userb1"))));
            get(token, "/api/v1/users?tenantId=" + B).andExpect(jsonPath("$[*].username", not(hasItem("userb1"))));
            get(token, "/api/v1/users/" + userB).andExpect(status().isNotFound());
        }
    }

    @Test void t12_to_t15_membersAndClaimsIsolation() throws Exception {
        UUID memB = members.findByTenantId(B).get(0).getId(), memA = members.findByTenantId(A).get(0).getId();
        UUID claimB = claims.findByTenantId(B).get(0).getId();
        for (String token : List.of(login("tenantadmin1", A), login("usera1", null))) {
            get(token, "/api/v1/members").andExpect(status().isOk()).andExpect(jsonPath("$[*].tenantId", everyItem(is(A.intValue()))));
            get(token, "/api/v1/members/" + memA).andExpect(status().isOk());
            get(token, "/api/v1/members/" + memB).andExpect(status().isNotFound());
            get(token, "/api/v1/claims").andExpect(jsonPath("$[*].tenantId", everyItem(is(A.intValue()))));
            get(token, "/api/v1/claims/" + claimB).andExpect(status().isNotFound());
        }
    }

    @Test void t16_invalidOrMissingJwt() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/members")).andExpect(status().isUnauthorized());
        get("garbage.token.value", "/api/v1/members").andExpect(status().isUnauthorized());
        String t = login("usera1", null);
        get(t.substring(0, t.length() - 4) + "AAAA", "/api/v1/members").andExpect(status().isUnauthorized());
    }

    @Test void t17_missingFeIs403() throws Exception {
        post(login("usera1", null), "/api/v1/users", Map.of("username", "x" + sfx(), "password", "password123")).andExpect(status().isForbidden());
        get(login("superadmin", null), "/api/v1/members").andExpect(status().isForbidden()); // platform admin has no tenant-data FE
        get(login("tenantadmin1", A), "/api/v1/tenants").andExpect(status().isForbidden());
    }

    @Test void t18_invalidDeIs403() throws Exception {
        User u = User.of("forged", "x", Role.TENANT_ADMIN, DeRole.PLATFORM); // FE ok, DE not
        get(jwtService.issue(u, null), "/api/v1/members").andExpect(status().isForbidden());
        User noTenant = User.of("forged2", "x", Role.TENANT_ADMIN, DeRole.OWN_TENANT); // DE ok, no active tenant
        get(jwtService.issue(noTenant, null), "/api/v1/members").andExpect(status().isForbidden());
    }

    @Test void t20_tokenTenantIdIsTenantServiceId() throws Exception {
        String t = login("usera1", null);
        String payload = new String(Base64.getUrlDecoder().decode(t.split("\\.")[1]));
        assertEquals(A.toString(), om.readTree(payload).get("tenantId").asText());
    }

    @Test void t21_onlyActiveTenantsCanBeUsed() throws Exception {
        Long suspended = tenants.findByName("Tenant S").get().tenantId();
        post(login("superadmin", null), "/api/v1/tenants/" + suspended + "/admins", Map.of("username", "adm-" + sfx(), "password", "password"))
            .andExpect(status().isConflict());
        Long t = newTenant("ACTIVE"); String admin = newAdmin(t);
        jdbc.update("update platform.tenants set status = 'SUSPENDED' where tenant_id = ?", t); // tenant-service suspends it
        loginAttempt(admin, "password", t).andExpect(status().isForbidden());
    }

    @Test void t22_adminDeactivatesAndReactivatesTenantUser() throws Exception {
        Long t = newTenant("ACTIVE"); String admin = newAdmin(t); String ta = login(admin, t);
        String name = "u-" + sfx();
        post(ta, "/api/v1/users", Map.of("username", name, "password", "password")).andExpect(status().isCreated());
        post(ta, "/api/v1/users/" + idOf(name) + "/deactivate", Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("INACTIVE")));
        loginAttempt(name, "password", null).andExpect(status().isUnauthorized());
        post(ta, "/api/v1/users/" + idOf(name) + "/reactivate", Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("ACTIVE")));
        loginAttempt(name, "password", null).andExpect(status().isOk());
    }

    @Test void t23_tenantUserCannotDeactivateOrReactivate() throws Exception {
        String ua = login("usera1", null);
        post(ua, "/api/v1/users/" + idOf("usera1") + "/deactivate", Map.of()).andExpect(status().isForbidden());
        post(ua, "/api/v1/users/" + idOf("usera1") + "/reactivate", Map.of()).andExpect(status().isForbidden());
        put(ua, "/api/v1/users/" + idOf("usera1"), Map.of("password", "newpassword1")).andExpect(status().isForbidden());
    }

    @Test void t24_adminCannotDeactivateThemselves() throws Exception {
        Long t = newTenant("ACTIVE"); String admin = newAdmin(t); newAdmin(t);
        post(login(admin, t), "/api/v1/users/" + idOf(admin) + "/deactivate", Map.of()).andExpect(status().isConflict());
    }

    @Test void t25_tenantKeepsAtLeastOneActiveAdmin() throws Exception {
        Long t = newTenant("ACTIVE"); String onlyAdmin = newAdmin(t);
        String ta = login(onlyAdmin, t); String plain = "u-" + sfx();
        post(ta, "/api/v1/users", Map.of("username", plain, "password", "password")).andExpect(status().isCreated());
        // Via the API the self-rule and active-caller check already stop this; the service invariant is the last line
        // of defence (e.g. against stale tokens), so call it directly as an active non-admin caller of the tenant.
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(idOf(plain).toString())
                .claim("deRole", "OWN_TENANT").claim("tenantId", t.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> userService.deactivate(idOf(onlyAdmin)));
            assertEquals(409, e.getStatusCode().value());
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertTrue(users.findByUsername(onlyAdmin).get().isActive());
    }

    @Test void t26_deactivatedAdminsOldTokenCannotManageUsers() throws Exception {
        Long t = newTenant("ACTIVE"); String a1 = newAdmin(t); String a2 = newAdmin(t);
        String a2Token = login(a2, t);
        post(login(a1, t), "/api/v1/users/" + idOf(a2) + "/deactivate", Map.of()).andExpect(status().isOk());
        post(a2Token, "/api/v1/users", Map.of("username", "u-" + sfx(), "password", "password")).andExpect(status().isForbidden());
        post(a2Token, "/api/v1/users/" + idOf(a1) + "/deactivate", Map.of()).andExpect(status().isForbidden());
    }

    @Test void t27_multiTenantAdminIsOnlyRemovedFromThisTenant() throws Exception {
        Long e = newTenant("ACTIVE"), f = newTenant("ACTIVE");
        String eAdmin = newAdmin(e); String shared = newAdmin(e); assign(shared, f);
        post(login(eAdmin, e), "/api/v1/users/" + idOf(shared) + "/deactivate", Map.of()).andExpect(status().isOk());
        User u = users.findByUsername(shared).get();
        assertTrue(u.isActive());
        assertEquals(Set.of(f), u.getTenantIds());
        loginAttempt(shared, "password", f).andExpect(status().isOk());
        loginAttempt(shared, "password", e).andExpect(status().isForbidden());
    }

    @Test void t28_adminCanResetTenantUserPasswordButNotOtherAdmins() throws Exception {
        Long t = newTenant("ACTIVE"); String a1 = newAdmin(t); String a2 = newAdmin(t); String ta = login(a1, t);
        String name = "u-" + sfx();
        post(ta, "/api/v1/users", Map.of("username", name, "password", "password")).andExpect(status().isCreated());
        put(ta, "/api/v1/users/" + idOf(name), Map.of("password", "changed123")).andExpect(status().isOk());
        loginAttempt(name, "changed123", null).andExpect(status().isOk());
        put(ta, "/api/v1/users/" + idOf(a2), Map.of("password", "changed123")).andExpect(status().isForbidden());
        put(ta, "/api/v1/users/" + idOf(a1), Map.of("password", "changed123")).andExpect(status().isOk());
    }
}
