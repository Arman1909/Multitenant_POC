package com.medimpact.mt.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class MemberDtos {

    private MemberDtos() {
    }

    /**
     * Create/update payload. Status is not settable here - it only changes through deactivate/reactivate,
     * which are TENANT_ADMIN-only. {@code version} is optional on update; when sent it is checked for optimistic locking.
     */
    public record MemberRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String memberNumber,
            @Past LocalDate dateOfBirth,
            @Size(max = 40) String insuranceNumber,
            @Size(max = 20) String carrierHqCode,
            Long version) {
    }

    public record MemberResponse(
            Long memberId,
            String memberNumber,
            String firstName,
            String lastName,
            String email,
            LocalDate dateOfBirth,
            String insuranceNumber,
            String carrierHqCode,
            MemberStatus status,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy,
            Long version) {

        public static MemberResponse from(Member m) {
            return new MemberResponse(m.getMemberId(), m.getMemberNumber(), m.getFirstName(), m.getLastName(),
                    m.getEmail(), m.getDateOfBirth(), m.getInsuranceNumber(), m.getCarrierHqCode(), m.getStatus(),
                    m.getCreatedAt(), m.getCreatedBy(), m.getUpdatedAt(), m.getUpdatedBy(), m.getVersion());
        }
    }
}
