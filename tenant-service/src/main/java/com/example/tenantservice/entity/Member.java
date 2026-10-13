package com.example.tenantservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Deliberately has NO `schema` attribute on @Table. The physical schema
 * (tenant_laker, tenant_medimpact, ...) is resolved per-request at runtime by
 * Hibernate's multi-tenancy SPI (CurrentTenantIdentifierResolverImpl +
 * MultiTenantConnectionProviderImpl), driven by TenantContext. This one
 * mapping serves every tenant schema.
 */
@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Member(String firstName, String lastName, String email, String createdBy) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }
}
