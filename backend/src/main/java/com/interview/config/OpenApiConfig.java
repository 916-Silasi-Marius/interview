package com.interview.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Swagger UI and API documentation.
 *
 * <p>Configures JWT Bearer authentication as the global security scheme
 * so the Swagger UI "Authorize" button can be used to test secured endpoints.
 * The login endpoint is excluded via {@code @SecurityRequirements} on the controller method.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Interview App API")
                        .description("Task management REST API with JWT authentication, "
                                + "employee management, and tag support.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Interview App")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
