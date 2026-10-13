package com.medimpact.mt.member;

import com.medimpact.mt.common.ApiException;
import com.medimpact.mt.common.PageResponse;
import com.medimpact.mt.context.CallerContextHolder;
import com.medimpact.mt.group.GroupMembershipRepository;
import com.medimpact.mt.member.MemberDtos.MemberRequest;
import com.medimpact.mt.member.MemberDtos.MemberResponse;
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
public class MemberService {

    private final MemberRepository memberRepository;
    private final GroupMembershipRepository membershipRepository;

    @Transactional
    public MemberResponse create(MemberRequest request) {
        CallerContextHolder.requireTenantSchema();
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("Member with email " + request.email() + " already exists");
        }
        if (request.memberNumber() != null && memberRepository.existsByMemberNumber(request.memberNumber())) {
            throw ApiException.conflict("Member number " + request.memberNumber() + " already exists");
        }
        Member member = new Member();
        apply(member, request);
        member.setStatus(MemberStatus.ACTIVE);
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long memberId) {
        return MemberResponse.from(load(memberId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> search(String q, MemberStatus status, Pageable pageable) {
        CallerContextHolder.requireTenantSchema();
        Specification<Member> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("memberNumber")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.of(memberRepository.findAll(spec, pageable), MemberResponse::from);
    }

    @Transactional
    public MemberResponse update(Long memberId, MemberRequest request) {
        Member member = load(memberId);
        if (request.version() != null && !Objects.equals(request.version(), member.getVersion())) {
            throw ApiException.conflict("Member " + memberId + " was modified (version " + member.getVersion()
                    + "); reload and retry");
        }
        if (memberRepository.existsByEmailIgnoreCaseAndMemberIdNot(request.email(), memberId)) {
            throw ApiException.conflict("Member with email " + request.email() + " already exists");
        }
        if (request.memberNumber() != null
                && memberRepository.existsByMemberNumberAndMemberIdNot(request.memberNumber(), memberId)) {
            throw ApiException.conflict("Member number " + request.memberNumber() + " already exists");
        }
        apply(member, request);
        return MemberResponse.from(memberRepository.saveAndFlush(member));
    }

    /** Soft delete: member becomes INACTIVE and all open group memberships are ended today. */
    @Transactional
    public void deactivate(Long memberId) {
        Member member = load(memberId);
        member.setStatus(MemberStatus.INACTIVE);
        LocalDate today = LocalDate.now();
        membershipRepository.findOpenByMemberId(memberId).forEach(m -> m.setEndDate(today));
    }

    /** Back to ACTIVE. Ended group memberships are NOT restored - re-add the member to groups explicitly. */
    @Transactional
    public MemberResponse reactivate(Long memberId) {
        Member member = load(memberId);
        member.setStatus(MemberStatus.ACTIVE);
        return MemberResponse.from(memberRepository.saveAndFlush(member));
    }

    private Member load(Long memberId) {
        CallerContextHolder.requireTenantSchema();
        return memberRepository.findById(memberId).orElseThrow(() -> ApiException.notFound("Member", memberId));
    }

    private static void apply(Member m, MemberRequest r) {
        m.setFirstName(r.firstName());
        m.setLastName(r.lastName());
        m.setEmail(r.email().trim());
        m.setMemberNumber(r.memberNumber() == null || r.memberNumber().isBlank() ? null : r.memberNumber().trim());
        m.setDateOfBirth(r.dateOfBirth());
        m.setInsuranceNumber(r.insuranceNumber());
        m.setCarrierHqCode(r.carrierHqCode());
    }
}
