package com.medimpact.mt.common;

import com.medimpact.mt.context.CallerContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.Instant;

/**
 * Audit columns shared by every tenant table, named like tenant-service's member table
 * (created_at / created_by / updated_at / updated_by / version). User comes from the JWT username.
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        String user = CallerContextHolder.currentUserOrSystem();
        createdAt = now;
        createdBy = user;
        updatedAt = now;
        updatedBy = user;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        updatedBy = CallerContextHolder.currentUserOrSystem();
    }
}
