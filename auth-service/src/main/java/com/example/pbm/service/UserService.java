package com.example.pbm.service;
import com.example.pbm.dto.request.CreateUserRequest;
import com.example.pbm.dto.request.UpdateUserRequest;
import com.example.pbm.dto.response.UserResponse;
import com.example.pbm.entity.*;
import com.example.pbm.repository.UserRepository;
import com.example.pbm.tenant.TenantContext;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TenantContext ctx;

    @Transactional
    public UserResponse create(CreateUserRequest r) {
        Long tenant = ctx.requireActiveTenant();
        requireActiveCaller(tenant);
        if (users.existsByUsername(r.username())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username exists");
        User u = User.of(r.username(), encoder.encode(r.password()), Role.TENANT_USER, DeRole.OWN_TENANT);
        u.getTenantIds().add(tenant); // exactly one tenant for a TENANT_USER
        return UserResponse.from(users.save(u));
    }
    public List<UserResponse> list() { return users.findByTenant(ctx.requireActiveTenant()).stream().map(UserResponse::from).toList(); }
    public UserResponse get(UUID id) { return UserResponse.from(load(id, ctx.requireActiveTenant())); }

    /** Password reset. A TENANT_ADMIN may reset TENANT_USERs of the active tenant and their own password, not other admins'. */
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest r) {
        Long tenant = ctx.requireActiveTenant();
        requireActiveCaller(tenant);
        User target = load(id, tenant);
        if (target.getRole() == Role.TENANT_ADMIN && !target.getId().equals(ctx.userId()))
            throw new AccessDeniedException("Cannot change another TENANT_ADMIN");
        target.setPassword(encoder.encode(r.password()));
        return UserResponse.from(users.save(target));
    }

    /**
     * Rules: an admin can't deactivate themselves, and a tenant must keep at least one active admin.
     * A TENANT_ADMIN who is assigned to other tenants too is only removed from THIS tenant (still active elsewhere);
     * everyone else is set INACTIVE and can no longer log in. Already-issued tokens stay valid until they expire (30 min).
     */
    @Transactional
    public UserResponse deactivate(UUID id) {
        Long tenant = ctx.requireActiveTenant();
        if (id.equals(ctx.userId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate yourself");
        requireActiveCaller(tenant);
        User target = load(id, tenant);
        if (!target.isActive()) return UserResponse.from(target);
        if (target.getRole() == Role.TENANT_ADMIN) {
            long otherActiveAdmins = users.lockActiveByTenantAndRole(tenant, Role.TENANT_ADMIN).stream()
                    .filter(a -> !a.getId().equals(id)).count();
            if (otherActiveAdmins == 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate the last active TENANT_ADMIN of the tenant");
            if (target.getTenantIds().size() > 1) {
                target.getTenantIds().remove(tenant);
                return UserResponse.from(users.save(target));
            }
        }
        target.setStatus("INACTIVE");
        return UserResponse.from(users.save(target));
    }

    @Transactional
    public UserResponse reactivate(UUID id) {
        Long tenant = ctx.requireActiveTenant();
        requireActiveCaller(tenant);
        User target = load(id, tenant);
        target.setStatus("ACTIVE");
        return UserResponse.from(users.save(target));
    }

    private User load(UUID id, Long tenant) {
        return users.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** JWTs live 30 min; make sure a caller deactivated/unassigned in the meantime can't keep managing users. */
    private void requireActiveCaller(Long tenant) {
        users.findByIdAndTenant(ctx.userId(), tenant).filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Your account is no longer active in this tenant"));
    }
}
