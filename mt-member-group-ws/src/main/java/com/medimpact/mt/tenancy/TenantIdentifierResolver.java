package com.medimpact.mt.tenancy;

import com.medimpact.mt.context.CallerContext;
import com.medimpact.mt.context.CallerContextHolder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/** Tells Hibernate which tenant schema the current session belongs to. */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    public static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        CallerContext ctx = CallerContextHolder.get();
        return ctx != null && ctx.schema() != null ? ctx.schema() : DEFAULT_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
