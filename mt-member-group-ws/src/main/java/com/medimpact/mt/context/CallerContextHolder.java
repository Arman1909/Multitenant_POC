package com.medimpact.mt.context;

import com.medimpact.mt.common.ApiException;
import org.springframework.http.HttpStatus;

/** Thread-bound caller context. Set by TenantContextFilter after JWT validation, cleared at the end of the request. */
public final class CallerContextHolder {

    private static final ThreadLocal<CallerContext> CURRENT = new ThreadLocal<>();

    private CallerContextHolder() {
    }

    public static void set(CallerContext context) {
        CURRENT.set(context);
    }

    public static CallerContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static CallerContext require() {
        CallerContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return ctx;
    }

    /** Schema of the caller's active tenant; fails if the token carries no tenant (e.g. SUPER_ADMIN). */
    public static String requireTenantSchema() {
        CallerContext ctx = require();
        if (ctx.schema() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No active tenant in token; tenant data is not accessible");
        }
        return ctx.schema();
    }

    /** Value written to created_by / updated_by. */
    public static String currentUserOrSystem() {
        CallerContext ctx = CURRENT.get();
        if (ctx == null) {
            return "system";
        }
        return ctx.username() != null ? ctx.username() : ctx.userId();
    }
}
