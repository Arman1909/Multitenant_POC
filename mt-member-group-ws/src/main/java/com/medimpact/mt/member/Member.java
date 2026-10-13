package com.medimpact.mt.member;

import com.medimpact.mt.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Maps the member table that tenant-service creates in every tenant schema
 * (db/migration/tenant/V1__create_member_table.sql). No schema on @Table: the connection's
 * search_path (set per request from the JWT tenant) decides which tenant's table is used.
 */
@Getter
@Setter
@Entity
@Table(name = "member")
public class Member extends AuditableEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MemberStatus status;

    @Column(name = "member_number", length = 30)
    private String memberNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "insurance_number", length = 40)
    private String insuranceNumber;

    @Column(name = "carrier_hq_code", length = 20)
    private String carrierHqCode;
}
