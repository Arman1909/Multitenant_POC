package com.example.tenantservice.repository;

import com.example.tenantservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * A single repository/mapping shared by every tenant. Which physical schema a
 * given call hits is decided entirely by whatever tenant identifier is set in
 * TenantContext at the time the Hibernate Session/connection is acquired -
 * this interface has no tenant-specific code whatsoever.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);
}
