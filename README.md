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

# Circuit Breaker Monitoring with K6 Performance Testing

This project includes a comprehensive monitoring system for circuit breakers with K6 performance testing capabilities.

## K6 Performance Testing

[K6](https://k6.io/) is a modern load testing tool that makes performance testing easy and productive for engineering teams. We've integrated K6 into our circuit breaker monitoring system to help you test how your circuit breakers behave under various load conditions.

### Test Scripts

The following test scripts are available in the `k6-scripts` directory:

- **load-test.js**: Basic load test with constant virtual users
- **stress-test.js**: Test with gradually increasing load to identify breaking points
- **spike-test.js**: Sudden traffic spikes to test how the system handles abrupt changes
- **soak-test.js**: Long-running test with consistent load to find issues that appear over time
- **circuit-breaker.js**: Specifically designed to test circuit breaker behavior

### Running Tests

#### Using Docker Compose

The easiest way to run K6 tests is through Docker Compose:

```bash
# Start all services including K6
docker-compose up -d

# Run a specific test from the running K6 container
docker-compose exec k6 k6 run /scripts/load-test.js

# Run with custom parameters
docker-compose exec k6 k6 run -e BASE_URL=http://host.docker.internal:8080 -e VUS=20 -e DURATION=60 /scripts/stress-test.js
```

#### Using Command Line Scripts

For your convenience, we've provided command line scripts to run K6 tests directly:

**Linux/macOS:**
```bash
cd k6-scripts
chmod +x run-k6-test.sh
./run-k6-test.sh --script stress-test.js --vus 50 --duration 60
```

**Windows:**
```cmd
cd k6-scripts
run-k6-test.bat --script spike-test.js --vus 30 --duration 45
```

#### Using the Web Interface

The circuit breaker dashboard includes a K6 Performance Testing page that allows you to:

1. Choose from predefined test templates
2. Customize test parameters
3. Run tests and view results in real-time
4. Analyze performance metrics with charts and detailed statistics

Access the K6 testing interface at: http://localhost:8080/k6-tests.html

### Test Parameters

You can customize the following parameters for all test scripts:

- **BASE_URL**: The base URL of your API (default: http://localhost:8080)
- **DURATION**: Test duration in seconds (default varies by test type)
- **VUS**: Number of virtual users (default varies by test type)
- **RAMPUP**: Ramp-up time in seconds (default: 5)

### Analyzing Results

K6 test results include:

- Success rate percentage
- Average response time
- Requests per second
- Total requests processed
- Response time distribution
- Detailed metrics with min/max/p95/p99 values

## Project Structure

- **Frontend/**: UI components for circuit breaker monitoring
- **src/**: Backend implementation
- **k6-scripts/**: K6 test scripts and utilities
- **results/**: Directory for storing test results

## Environment Setup

The project uses Docker Compose to set up the required services:

```bash
# Start all services
docker-compose up -d
```

Key services:
- Circuit breaker monitoring API
- Kafka for event streaming
- Redis for caching
- Prometheus for metrics collection
- Grafana for visualization
- K6 for performance testing
- Jaeger for distributed tracing

## Troubleshooting

If you encounter issues with K6 testing:

1. Make sure the K6 service is running: `docker-compose ps`
2. Check that your API is accessible from the K6 container
3. Verify that the test scripts exist in the correct location
4. Examine the test output for specific errors

For more help, refer to the [K6 documentation](https://k6.io/docs/). 