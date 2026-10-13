package com.example.pbm.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String username;
    @Column(nullable = false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DeRole deRole;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(nullable = false) private Instant createdAt = Instant.now();
    /** user_tenants(user_id, tenant_id) - tenant_id is tenant-service's platform.tenants.tenant_id */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_tenants", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "tenant_id", nullable = false)
    private Set<Long> tenantIds = new HashSet<>();
    public static User of(String username, String encodedPassword, Role role, DeRole de) {
        User u = new User(); u.id = UUID.randomUUID(); u.username = username; u.password = encodedPassword; u.role = role; u.deRole = de; return u;
    }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
