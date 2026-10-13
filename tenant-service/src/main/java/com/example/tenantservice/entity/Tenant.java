package com.example.tenantservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lives in the fixed `platform` schema - this entity is NEVER subject to
 * multi-tenant schema routing (see MultiTenancyConfig / TenantIdentifierResolverImpl,
 * which only affects entities resolved through the tenant-routed persistence unit).
 */
@Entity
@Table(name = "tenants", schema = "platform")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "tenant_name", nullable = false, unique = true, length = 120)
    private String tenantName;

    @Column(name = "tenant_desc", length = 500)
    private String tenantDesc;

    @Column(name = "schema_name", nullable = false, unique = true, length = 63, updatable = false)
    private String schemaName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "tenant_address1", length = 200)
    private String tenantAddress1;

    @Column(name = "tenant_address2", length = 200)
    private String tenantAddress2;

    @Column(name = "tenant_city", length = 100)
    private String tenantCity;

    @Column(name = "tenant_state", length = 100)
    private String tenantState;

    @Column(name = "tenant_postal_code", length = 20)
    private String tenantPostalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TenantStatus status;

    @Column(name = "entered", nullable = false, updatable = false)
    private Instant entered;

    @Column(name = "entered_by", nullable = false, updatable = false, length = 100)
    private String enteredBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Tenant newProvisioning(String tenantName, String tenantDesc, String schemaName,
                                          LocalDate startDate, LocalDate endDate,
                                          String address1, String address2, String city,
                                          String state, String postalCode, String enteredBy) {
        Tenant t = new Tenant();
        t.tenantName = tenantName;
        t.tenantDesc = tenantDesc;
        t.schemaName = schemaName;
        t.startDate = startDate;
        t.endDate = endDate;
        t.tenantAddress1 = address1;
        t.tenantAddress2 = address2;
        t.tenantCity = city;
        t.tenantState = state;
        t.tenantPostalCode = postalCode;
        t.status = TenantStatus.PROVISIONING;
        Instant now = Instant.now();
        t.entered = now;
        t.enteredBy = enteredBy;
        t.updatedAt = now;
        t.updatedBy = enteredBy;
        return t;
    }

    public void touch(String username) {
        this.updatedAt = Instant.now();
        this.updatedBy = username;
    }
}
