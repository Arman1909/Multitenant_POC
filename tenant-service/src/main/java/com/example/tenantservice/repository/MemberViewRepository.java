package com.example.tenantservice.repository;

import com.example.tenantservice.entity.MemberView;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface MemberViewRepository extends Repository<MemberView, Long> {

    List<MemberView> findAllByOrderByMemberIdAsc();

    Optional<MemberView> findByMemberId(Long memberId);
}