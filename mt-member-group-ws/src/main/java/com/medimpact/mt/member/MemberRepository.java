package com.medimpact.mt.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndMemberIdNot(String email, Long memberId);

    boolean existsByMemberNumber(String memberNumber);

    boolean existsByMemberNumberAndMemberIdNot(String memberNumber, Long memberId);
}
