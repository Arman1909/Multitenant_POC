package com.example.pbm.service;
import com.example.pbm.entity.*;
import com.example.pbm.repository.*;
import com.example.pbm.tenant.TenantDirectory;
import com.example.pbm.tenant.TenantInfo;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds superadmin, plus demo users for the first two ACTIVE tenants that tenant-service has created
 * ("A" = first, "B" = second). All passwords: "password". Tenants are never created here.
 */
@Slf4j @Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users; private final TenantDirectory tenants;
    private final MemberRepository members; private final ClaimRepository claims;
    private final PasswordEncoder encoder;
    @Value("${app.seed-demo:true}") private boolean demo;

    @Override public void run(String... args) {
        if (!users.existsByUsername("superadmin")) user("superadmin", Role.SUPER_ADMIN, DeRole.PLATFORM);
        if (!demo || users.existsByUsername("tenantadmin1")) return;
        List<TenantInfo> active = tenants.findAll().stream().filter(TenantInfo::isActive).limit(2).toList();
        if (active.size() < 2) {
            log.warn("Demo users not seeded: need 2 ACTIVE tenants in platform.tenants (found {}). "
                    + "Create them in tenant-service and restart auth-service.", active.size());
            return;
        }
        TenantInfo a = active.get(0), b = active.get(1);
        user("tenantadmin1", Role.TENANT_ADMIN, DeRole.OWN_TENANT, a.tenantId(), b.tenantId());
        user("usera1", Role.TENANT_USER, DeRole.OWN_TENANT, a.tenantId());
        user("userb1", Role.TENANT_USER, DeRole.OWN_TENANT, b.tenantId());
        for (TenantInfo t : active) {
            Member m = members.save(new Member(UUID.randomUUID(), t.tenantId(), "Member of " + t.tenantName()));
            members.save(new Member(UUID.randomUUID(), t.tenantId(), "Second member of " + t.tenantName()));
            claims.save(new Claim(UUID.randomUUID(), t.tenantId(), m.getId(), new BigDecimal("125.50"), "PAID"));
        }
        log.info("Seeded demo users: tenantadmin1 ({} + {}), usera1 ({}), userb1 ({})",
                a.tenantName(), b.tenantName(), a.tenantName(), b.tenantName());
    }
    private void user(String name, Role r, DeRole de, Long... tenantIds) {
        User u = User.of(name, encoder.encode("password"), r, de);
        for (Long t : tenantIds) u.getTenantIds().add(t);
        users.save(u);
    }
}
