package com.medimpact.mt.security;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.medimpact.mt.security.Fe.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Locks in the agreed permission matrix for the member/group service. */
class RolePermissionMatrixTest {

    @Test
    void superAdminNeverGetsTenantBusinessData() {
        assertThat(Role.SUPER_ADMIN.fe()).isEmpty();
    }

    @Test
    void tenantUserDoesDayToDayCrudButNoDeactivateReactivateOrAudit() {
        assertThat(Role.TENANT_USER.fe()).containsExactlyInAnyOrder(
                MEMBER_READ, MEMBER_CREATE, MEMBER_UPDATE,
                GROUP_READ, GROUP_CREATE, GROUP_UPDATE,
                GROUP_MEMBER_MANAGE);
    }

    @Test
    void tenantAdminIsSupersetOfTenantUserPlusAdminOnlyActions() {
        Set<Fe> adminOnly = EnumSet.of(MEMBER_DEACTIVATE, MEMBER_REACTIVATE,
                GROUP_DEACTIVATE, GROUP_REACTIVATE, AUDIT_READ);
        assertThat(Role.TENANT_ADMIN.fe()).containsAll(Role.TENANT_USER.fe()).containsAll(adminOnly);
        assertThat(Role.TENANT_USER.fe()).doesNotContainAnyElementsOf(adminOnly);
    }

    @Test
    void everyPermissionIsUsedByAtLeastOneRule() {
        Set<Fe> used = EnumSet.noneOf(Fe.class);
        AccessRules.RULES.forEach(r -> used.add(r.fe()));
        used.add(AUDIT_READ); // checked in code for includeHistory=true
        assertThat(used).containsExactlyInAnyOrder(Fe.values());
    }
}
