package com.example.tenantservice.integration;

import com.example.tenantservice.dto.TenantCreateRequest;
import com.example.tenantservice.entity.Member;
import com.example.tenantservice.repository.MemberRepository;
import com.example.tenantservice.repository.TenantRepository;
import com.example.tenantservice.security.TestTokens;
import com.example.tenantservice.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end proof of:
 *  - POST /api/v1/tenants provisions a real schema + member table via Flyway
 *  - the tenant only becomes ACTIVE after provisioning succeeds
 *  - the SAME MemberRepository/Member mapping transparently reads/writes
 *    different physical schemas depending on TenantContext, with zero
 *    cross-tenant leakage
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TenantProvisioningIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Tenant APIs need a SUPER_ADMIN JWT signed with the shared secret (same as auth-service issues). */
    private static final String SUPER_ADMIN = TestTokens.bearer(TestTokens.superAdmin());

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void provisionsSchemaAndRoutesMemberQueriesPerTenant() throws Exception {
        // --- Provision "Laker" via the real HTTP API ---
        String lakerBody = objectMapper.writeValueAsString(
                new TenantCreateRequest("Laker", "Laker tenant", null, null,
                        null, null, null, null, null));

        String lakerJson = mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", SUPER_ADMIN)
                        .contentType("application/json")
                        .content(lakerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.schemaName", is("tenant_laker")))
                .andReturn().getResponse().getContentAsString();

        // --- Provision "MedImpact" too ---
        String medBody = objectMapper.writeValueAsString(
                new TenantCreateRequest("MedImpact", "MedImpact tenant", null, null,
                        null, null, null, null, null));

        mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", SUPER_ADMIN)
                        .contentType("application/json")
                        .content(medBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.schemaName", is("tenant_medimpact")));

        // --- Verify physical schema + table actually exist ---
        Integer schemaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name IN ('tenant_laker','tenant_medimpact')",
                Integer.class);
        assertThat(schemaCount).isEqualTo(2);

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'tenant_laker' AND table_name = 'member'",
                Integer.class);
        assertThat(tableCount).isEqualTo(1);

        // --- Prove Hibernate routing: same repository, different schemas ---
        try {
            TenantContext.setCurrentTenant("tenant_laker");
            memberRepository.save(new Member("Kobe", "Bryant", "kobe@laker.example", "alice"));
        } finally {
            TenantContext.clear();
        }

        try {
            TenantContext.setCurrentTenant("tenant_medimpact");
            memberRepository.save(new Member("Jane", "Doe", "jane@medimpact.example", "alice"));
        } finally {
            TenantContext.clear();
        }

        Long lakerMemberCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tenant_laker.member", Long.class);
        Long medMemberCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tenant_medimpact.member", Long.class);

        assertThat(lakerMemberCount).isEqualTo(1);
        assertThat(medMemberCount).isEqualTo(1);

        // No cross-tenant leakage: laker schema must not contain medimpact's row.
        Integer leaked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tenant_laker.member WHERE email = 'jane@medimpact.example'",
                Integer.class);
        assertThat(leaked).isZero();
    }

    @Test
    void suspendRetainsDataAndReactivateRestoresAccess() throws Exception {
        String body = objectMapper.writeValueAsString(
                new TenantCreateRequest("Bayvrio", "Bayvrio tenant", null, null,
                        null, null, null, null, null));

        String json = mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", SUPER_ADMIN)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long tenantId = objectMapper.readTree(json).get("tenantId").asLong();

        mockMvc.perform(post("/api/v1/tenants/" + tenantId + "/suspend").header("Authorization", SUPER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));

        Integer tableStillExists = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'tenant_bayvrio' AND table_name = 'member'",
                Integer.class);
        assertThat(tableStillExists).isEqualTo(1); // data retained

        mockMvc.perform(post("/api/v1/tenants/" + tenantId + "/reactivate").header("Authorization", SUPER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}
