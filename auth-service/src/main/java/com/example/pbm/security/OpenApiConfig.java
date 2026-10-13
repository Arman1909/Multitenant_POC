package com.example.pbm.security;

import com.example.pbm.entity.Role;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("PBM Auth POC").version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))   // lock on all endpoints
                .components(new Components().addSecuritySchemes("bearerAuth",       // what the lock opens
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /** Titles and descriptions come from AccessRules, the same list SecurityConfig enforces. */
    @Bean
    OpenApiCustomizer roleLabels() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            if (path.equals("/api/v1/auth/login")) {
                op.setSecurity(List.of());                       // public: no lock
                op.setSummary("[PUBLIC] login");
                return;
            }
            if (path.equals("/api/v1/auth/me")) {
                op.setSummary("[LOGIN REQUIRED] me");
                return;
            }
            if (path.equals("/api/v1/auth/switch-tenant")) {
                op.setSummary("[TENANT_ADMIN] switchTenant");
                return;
            }
            for (AccessRules.Rule r : AccessRules.RULES) {
                if (r.pattern().equals(path) && r.method().name().equals(method.name())) {
                    List<String> roles = Arrays.stream(Role.values())
                            .filter(x -> x.fe().contains(r.fe())).map(Enum::name).toList();
                    String name = op.getOperationId() == null ? "" : op.getOperationId().replaceAll("_\\d+$", "");
                    op.setSummary("[" + String.join(" / ", roles) + "] " + name);
                    op.setDescription("**Allowed roles:** " + roles + "  \n**Permission:** " + r.fe());
                }
            }
        }));
    }
}