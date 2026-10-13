package com.example.pbm.service;
import com.example.pbm.dto.request.*;
import com.example.pbm.dto.response.*;
import com.example.pbm.entity.*;
import com.example.pbm.repository.*;
import com.example.pbm.tenant.TenantDirectory;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Tenants themselves are created/managed in tenant-service; here SUPER_ADMIN lists them and assigns admins. */
@Service @RequiredArgsConstructor
public class TenantService {
    private final TenantDirectory tenants;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public List<TenantResponse> list() { return tenants.findAll().stream().map(TenantResponse::from).toList(); }

    @Transactional
    public UserResponse assignAdmin(Long tenantId, AssignTenantAdminRequest r) {
        var tenant = tenants.find(tenantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (!tenant.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant is " + tenant.status());
        User u = users.findByUsername(r.username()).orElse(null);
        if (u == null) {
            if (r.password() == null) throw new IllegalArgumentException("password required for a new admin");
            u = User.of(r.username(), encoder.encode(r.password()), Role.TENANT_ADMIN, DeRole.OWN_TENANT);
        } else if (u.getRole() != Role.TENANT_ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing user is not a TENANT_ADMIN");
        }
        u.getTenantIds().add(tenantId);
        return UserResponse.from(users.save(u));
    }
}
