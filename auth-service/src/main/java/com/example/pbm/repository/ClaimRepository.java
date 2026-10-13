package com.example.pbm.repository;
import com.example.pbm.entity.Claim;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByTenantId(Long tenantId);
    Optional<Claim> findByIdAndTenantId(UUID id, Long tenantId);
}
