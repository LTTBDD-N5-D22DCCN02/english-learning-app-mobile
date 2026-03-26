package com.estudy.backend.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "EStudy API",
                version     = "1.0.0",
                description = "REST API for EStudy App",
                contact     = @Contact(
                        name  = "EStudy",
                        email = "support@estudy.com"
                )
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        servers = {
                @Server(description = "Local development server")
        }
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        description  = "Nhập JWT token lấy từ POST /auth/login. Format: Bearer <token>"
)
public class OpenApiConfig {
}
