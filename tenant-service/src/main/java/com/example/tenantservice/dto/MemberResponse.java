package com.example.tenantservice.dto;

import com.example.tenantservice.entity.MemberView;

import java.time.LocalDate;

public record MemberResponse(
        Long memberId, String memberNumber, String firstName, String lastName,
        LocalDate dateOfBirth, String email, String carrierHqCode,
        String insuranceNumber, String status) {

    public static MemberResponse from(MemberView m) {
        return new MemberResponse(m.getMemberId(), m.getMemberNumber(), m.getFirstName(),
                m.getLastName(), m.getDateOfBirth(), m.getEmail(), m.getCarrierHqCode(),
                m.getInsuranceNumber(), m.getStatus());
    }
}