package com.example.tenantservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** "Authorize" button in Swagger UI: paste the accessToken from auth-service's /api/v1/auth/login. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI tenantServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenant Web Service")
                        .description("Shared-database, schema-per-tenant provisioning API. "
                                + "Send the auth-service JWT as a Bearer token: tenant APIs need SUPER_ADMIN.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
