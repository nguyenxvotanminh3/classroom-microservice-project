package com.kienlongbank.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8090}")
    private int serverPort;

    @Bean
    @Primary
    public OpenAPI apiGatewayOpenAPI() {
        Server apiGatewayServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("API Gateway Server");
                
        Server userServiceServer = new Server()
                .url("http://localhost:8080")
                .description("User Service");
                
        Server securityServiceServer = new Server()
                .url("http://localhost:8081")
                .description("Security Service");
                
        Server classroomServiceServer = new Server()
                .url("http://localhost:8082")
                .description("Classroom Service");
                
        Server emailServiceServer = new Server()
                .url("http://localhost:8083")
                .description("Email Service");
                
        return new OpenAPI()
                .info(new Info()
                        .title("Microservices API Documentation")
                        .description("API Documentation for all microservices")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Kienlongbank")
                                .email("info@kienlongbank.com")
                                .url("https://kienlongbank.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(Arrays.asList(
                        apiGatewayServer,
                        userServiceServer,
                        securityServiceServer,
                        classroomServiceServer,
                        emailServiceServer));
    }
} 