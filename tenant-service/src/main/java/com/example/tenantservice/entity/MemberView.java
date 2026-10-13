package com.example.tenantservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "member_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberView {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_number")
    private String memberNumber;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "email")
    private String email;

    @Column(name = "carrier_hq_code")
    private String carrierHqCode;

    @Column(name = "insurance_number")
    private String insuranceNumber;

    @Column(name = "status")
    private String status;
}