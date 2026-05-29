package com.taxauthority.debtrecovery.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI taxDebtRecoveryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tax Collection and Debt Recovery API")
                .description("REST API for managing tax prepayments, credits, and debt settlement - Context: ctx_99a057884357")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Tax Authority")
                    .email("support@taxauthority.gov")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server")
            ));
    }
}

// Made with Bob
