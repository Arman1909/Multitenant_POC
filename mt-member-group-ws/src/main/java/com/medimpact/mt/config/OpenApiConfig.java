package com.medimpact.mt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Adds an "Authorize" button to Swagger UI: paste the accessToken from auth-service's /api/v1/auth/login. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI memberGroupOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multi-tenant Member & Group WS")
                        .version("v1")
                        .description("Schema-per-tenant member and group APIs. Send the auth-service JWT as a Bearer token; "
                                + "the tenant is taken from its tenantId claim."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
