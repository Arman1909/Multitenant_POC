package com.medimpact.mt.group;

import com.medimpact.mt.common.ApiException;
import com.medimpact.mt.common.PageResponse;
import com.medimpact.mt.context.CallerContextHolder;
import com.medimpact.mt.group.GroupDtos.AddMemberRequest;
import com.medimpact.mt.group.GroupDtos.GroupRequest;
import com.medimpact.mt.group.GroupDtos.GroupResponse;
import com.medimpact.mt.group.GroupDtos.MembershipResponse;
import com.medimpact.mt.member.Member;
import com.medimpact.mt.member.MemberRepository;
import com.medimpact.mt.member.MemberStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GroupResponse create(GroupRequest request) {
        CallerContextHolder.requireTenantSchema();
        if (groupRepository.existsByGroupNumber(request.groupNumber())) {
            throw ApiException.conflict("Group number " + request.groupNumber() + " already exists");
        }
        MemberGroup group = new MemberGroup();
        apply(group, request);
        group.setStatus(GroupStatus.ACTIVE);
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public GroupResponse get(Long groupId) {
        return GroupResponse.from(loadGroup(groupId));
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> search(String q, GroupStatus status, Pageable pageable) {
        CallerContextHolder.requireTenantSchema();
        Specification<MemberGroup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("groupNumber")), like),
                        cb.like(cb.lower(root.get("groupName")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.of(groupRepository.findAll(spec, pageable), GroupResponse::from);
    }

    @Transactional
    public GroupResponse update(Long groupId, GroupRequest request) {
        MemberGroup group = loadGroup(groupId);
        if (request.version() != null && !Objects.equals(request.version(), group.getVersion())) {
            throw ApiException.conflict("Group " + groupId + " was modified (version " + group.getVersion()
                    + "); reload and retry");
        }
        if (groupRepository.existsByGroupNumberAndGroupIdNot(request.groupNumber(), groupId)) {
            throw ApiException.conflict("Group number " + request.groupNumber() + " already exists");
        }
        apply(group, request);
        return GroupResponse.from(groupRepository.saveAndFlush(group));
    }

    /** Soft delete: group becomes INACTIVE and all its open memberships are ended today. */
    @Transactional
    public void deactivate(Long groupId) {
        MemberGroup group = loadGroup(groupId);
        LocalDate today = LocalDate.now();
        group.setStatus(GroupStatus.INACTIVE);
        if (group.getTerminationDate() == null) {
            group.setTerminationDate(today);
        }
        membershipRepository.findOpenByGroupId(groupId).forEach(m -> m.setEndDate(today));
    }

    /** Back to ACTIVE and clears the termination date. Ended memberships are NOT restored. */
    @Transactional
    public GroupResponse reactivate(Long groupId) {
        MemberGroup group = loadGroup(groupId);
        group.setStatus(GroupStatus.ACTIVE);
        group.setTerminationDate(null);
        return GroupResponse.from(groupRepository.saveAndFlush(group));
    }

    @Transactional
    public MembershipResponse addMember(Long groupId, AddMemberRequest request) {
        MemberGroup group = loadGroup(groupId);
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw ApiException.badRequest("Group " + groupId + " is not active");
        }
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> ApiException.notFound("Member", request.memberId()));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw ApiException.badRequest("Member " + member.getMemberId() + " is not active");
        }
        if (membershipRepository.findOpen(groupId, member.getMemberId()).isPresent()) {
            throw ApiException.conflict("Member " + member.getMemberId() + " is already in group " + groupId);
        }
        GroupMembership membership = new GroupMembership();
        membership.setGroup(group);
        membership.setMember(member);
        membership.setStartDate(request.startDate() != null ? request.startDate() : LocalDate.now());
        return MembershipResponse.from(membershipRepository.save(membership));
    }

    /** Ends the member's current membership in the group (history is kept). */
    @Transactional
    public void removeMember(Long groupId, Long memberId) {
        loadGroup(groupId);
        GroupMembership membership = membershipRepository.findOpen(groupId, memberId)
                .orElseThrow(() -> ApiException.notFound("Current membership of member " + memberId + " in group", groupId));
        LocalDate today = LocalDate.now();
        membership.setEndDate(membership.getStartDate().isAfter(today) ? membership.getStartDate() : today);
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> membersOfGroup(Long groupId, boolean includeHistory) {
        loadGroup(groupId);
        return membershipRepository.findByGroup(groupId, includeHistory).stream().map(MembershipResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> membershipsOfMember(Long memberId, boolean includeHistory) {
        CallerContextHolder.requireTenantSchema();
        if (!memberRepository.existsById(memberId)) {
            throw ApiException.notFound("Member", memberId);
        }
        return membershipRepository.findByMember(memberId, includeHistory).stream().map(MembershipResponse::from).toList();
    }

    private MemberGroup loadGroup(Long groupId) {
        CallerContextHolder.requireTenantSchema();
        return groupRepository.findById(groupId).orElseThrow(() -> ApiException.notFound("Group", groupId));
    }

    private static void apply(MemberGroup g, GroupRequest r) {
        if (r.effectiveDate() != null && r.terminationDate() != null && r.terminationDate().isBefore(r.effectiveDate())) {
            throw ApiException.badRequest("terminationDate must not be before effectiveDate");
        }
        g.setGroupNumber(r.groupNumber().trim());
        g.setGroupName(r.groupName());
        g.setGroupDesc(r.groupDesc());
        g.setEffectiveDate(r.effectiveDate());
        g.setTerminationDate(r.terminationDate());
    }
}
