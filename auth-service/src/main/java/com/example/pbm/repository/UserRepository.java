package com.example.pbm.repository;
import com.example.pbm.entity.Role;
import com.example.pbm.entity.User;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("select u from User u join u.tenantIds t where t = :tid")
    List<User> findByTenant(@Param("tid") Long tenantId);
    @Query("select u from User u join u.tenantIds t where t = :tid and u.id = :id")
    Optional<User> findByIdAndTenant(@Param("id") UUID id, @Param("tid") Long tenantId);
    /** Locks the tenant's active users of a role so two admins can't deactivate each other at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u join u.tenantIds t where t = :tid and u.role = :role and u.status = 'ACTIVE'")
    List<User> lockActiveByTenantAndRole(@Param("tid") Long tenantId, @Param("role") Role role);
}
