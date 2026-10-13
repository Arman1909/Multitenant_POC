package com.example.tenantservice.repository;

import com.example.tenantservice.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantNameIgnoreCase(String tenantName);

    boolean existsByTenantNameIgnoreCase(String tenantName);

    boolean existsBySchemaName(String schemaName);

    // Row-level lock used during suspend/reactivate/deactivate transitions to
    // serialize concurrent status changes on the same tenant.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Tenant> findByTenantId(Long tenantId);
}
