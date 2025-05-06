package com.kienlongbank.classroomservice.config;

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

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI classroomServiceOpenAPI() {
        Server mainServer = new Server()
                .url("http://localhost:8082")
                .description("Classroom Service - Direct");
        
        Server apiServer = new Server()
                .url("http://localhost:8082/api")
                .description("Classroom Service - with /api context");
                
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
                
        return new OpenAPI()
                .servers(Arrays.asList(mainServer, apiServer))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("Classroom Service API")
                        .description("API for managing classrooms and student enrollments")
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