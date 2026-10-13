package com.example.tenantservice.tenant;

/**
 * Holds the currently-active tenant schema for the executing thread.
 *
 * Contract:
 *  - set() must be called before any tenant-scoped repository call.
 *  - clear() MUST be called in a finally block once the request/unit of work
 *    completes, or a pooled thread (Tomcat worker) could leak a tenant
 *    identifier into an unrelated later request.
 *  - Never propagates automatically across @Async / new threads - if async
 *    tenant-scoped work is added later it must explicitly copy the value.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(String tenantSchema) {
        CURRENT_TENANT.set(tenantSchema);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
