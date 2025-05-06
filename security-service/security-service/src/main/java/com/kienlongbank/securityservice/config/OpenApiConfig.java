package com.kienlongbank.securityservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Collections;

@Configuration
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI securityServiceOpenAPI() {
        Server apiServer = new Server()
                .url("http://localhost:8081")
                .description("Security Service API Server");
        
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
                
        return new OpenAPI()
                .servers(Collections.singletonList(apiServer))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("Security Service API")
                        .description("RESTful API for Authentication and Authorization")
                        .version("1.0")
                        .contact(new Contact()
                                .name("KienLongBank")
                                .url("https://kienlongbank.com")
                                .email("info@kienlongbank.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
} 