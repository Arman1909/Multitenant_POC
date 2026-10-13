package com.medimpact.mt.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class GroupDtos {

    private GroupDtos() {
    }

    /** Status is not settable here - it only changes through deactivate/reactivate (TENANT_ADMIN-only). */
    public record GroupRequest(
            @NotBlank @Size(max = 50) String groupNumber,
            @NotBlank @Size(max = 150) String groupName,
            @Size(max = 500) String groupDesc,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Long version) {
    }

    public record GroupResponse(
            Long groupId,
            String groupNumber,
            String groupName,
            String groupDesc,
            GroupStatus status,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Instant createdAt,
            String createdBy,
            Instant updatedAt,
            String updatedBy,
            Long version) {

        public static GroupResponse from(MemberGroup g) {
            return new GroupResponse(g.getGroupId(), g.getGroupNumber(), g.getGroupName(), g.getGroupDesc(),
                    g.getStatus(), g.getEffectiveDate(), g.getTerminationDate(), g.getCreatedAt(), g.getCreatedBy(),
                    g.getUpdatedAt(), g.getUpdatedBy(), g.getVersion());
        }
    }

    /** startDate defaults to today. */
    public record AddMemberRequest(@NotNull Long memberId, LocalDate startDate) {
    }

    public record MembershipResponse(
            Long membershipId,
            Long groupId,
            String groupNumber,
            String groupName,
            Long memberId,
            String memberNumber,
            String memberName,
            LocalDate startDate,
            LocalDate endDate,
            boolean current) {

        public static MembershipResponse from(GroupMembership gm) {
            var g = gm.getGroup();
            var m = gm.getMember();
            return new MembershipResponse(gm.getMembershipId(), g.getGroupId(), g.getGroupNumber(), g.getGroupName(),
                    m.getMemberId(), m.getMemberNumber(), m.getFirstName() + " " + m.getLastName(),
                    gm.getStartDate(), gm.getEndDate(), gm.getEndDate() == null);
        }
    }
}
