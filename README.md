# Microservices Learning Project

This project is a microservices-based application built with Spring Boot and Spring Cloud. It demonstrates various microservices patterns and best practices.

## Services

- **API Gateway**: Entry point for all client requests, handles routing and authentication
- **User Service**: Manages user-related operations
- **Security Service**: Handles authentication and authorization
- **Classroom Service**: Manages classroom-related operations
- **Email Service**: Handles email notifications

## Technologies

- Spring Boot
- Spring Cloud Gateway
- Spring Security
- Apache Dubbo
- HashiCorp Vault
- OpenTelemetry
- Prometheus
- Docker
- Maven

## Features

- API Gateway with centralized routing
- Authentication and Authorization
- Distributed Tracing
- Metrics and Monitoring
- Internationalization (i18n)
- API Documentation (OpenAPI/Swagger)
- Access Logging
- Error Handling
- Unit and Integration Testing

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- Docker and Docker Compose
- HashiCorp Vault

### Running the Application

1. Start the infrastructure services:
```bash
docker-compose up -d
```

2. Initialize and unseal Vault (first time only):
```bash
# Initialize Vault
vault operator init

# Unseal Vault (need to be done after every restart)
vault operator unseal
```

3. Build all services:
```bash
mvn clean install
```

4. Start each service:
```bash
# Start in this order:
# 1. API Gateway
# 2. Security Service
# 3. User Service
# 4. Classroom Service
# 5. Email Service

cd [service-directory]
mvn spring-boot:run
```

### API Documentation

After starting the services, you can access the API documentation at:
- API Gateway Swagger UI: http://localhost:8090/swagger-ui.html
- Individual service Swagger UIs are available at their respective ports

## Project Structure

```
.
├── api-gateway/                 # API Gateway Service
├── security-service/           # Authentication Service
├── user-service/              # User Management Service
├── classroom-service/         # Classroom Management Service
├── email-service/            # Email Notification Service
├── docs/                    # Project Documentation
└── docker-compose.yml      # Docker Compose Configuration
```

## Documentation

Detailed documentation for various aspects of the project can be found in the `docs/` directory:

- [Access Logging](docs/AccessLog.md)
- [API Documentation](docs/ApiDocs.md)
- [Authentication](docs/Security.md)
- [Caching](docs/Caching.md)
- [Error Handling](docs/Handler.md)
- [Internationalization](docs/I18n.md)
- [Metrics & Monitoring](docs/Metrics.md)
- [Testing](docs/UnitTest.md)
- [Tracing](docs/Tracing.md)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details 