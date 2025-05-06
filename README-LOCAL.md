# Running Services Locally

This document provides instructions for running all services locally without Docker.

## Prerequisites

Before you start, make sure you have the following:

1. MySQL Server running on port 3306
2. Redis Server running on port 6379
3. ZooKeeper running on port 2181
4. Kafka running on port 9092
5. Java 17 or higher
6. Maven

## Database Setup

The application is now configured to automatically create the required databases if they don't exist:

- user_write_db
- user_read_db1
- user_read_db2
- classroom_read_db1
- nguyenminh_classroom
- security_db
- email_db

Ensure MySQL is running and that the root user credentials are configured correctly in each service's application-local.properties file. The connection URL now includes:

```
?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

## Service Configuration

Each service has a dedicated `application-local.properties` file for running locally, with enhanced database connection settings. To use it, start each service with the `local` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Service Launch Order

Start the services in the following order:

1. ZooKeeper and Kafka (if not already running)
   ```
   docker run -d --name zookeeper-local -p 2181:2181 zookeeper:3.8
   docker run -d --name kafka-local --link zookeeper-local:zookeeper -p 9092:9092 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper-local:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -e KAFKA_BROKER_ID=1 confluentinc/cp-kafka:7.3.0
   ```

2. MySQL and Redis (if not already running)
   ```
   docker run -d --name mysql-local -p 3306:3306 -e MYSQL_ROOT_PASSWORD=Mink281104@ mysql:8.0
   docker run -d --name redis-local -p 6379:6379 redis:latest
   ```

3. Security Service
   ```
   cd security-service/security-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. User Service
   ```
   cd user-service/user-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

5. Classroom Service
   ```
   cd classroom-service/classroomservice
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

6. Email Service
   ```
   cd email-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

7. API Gateway
   ```
   cd api-gateway
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

## Service Access

After starting all services, you can access them at:

- API Gateway: http://localhost:8090
- Security Service: http://localhost:8081/api
- User Service: http://localhost:8080/api
- Classroom Service: http://localhost:8082/api
- Email Service: http://localhost:8083/api

## Login API

To test the login endpoint, use:

```
POST http://localhost:8090/security-api/auth/login
```

With request body:
```json
{
  "username": "nguyenxvotanminh",
  "password": "yourpassword"
}
```

## Troubleshooting

### Database Connection Issues

If you see database connection errors:

1. Verify MySQL is running:
   ```
   docker ps | grep mysql-local
   ```

2. Check the database connection parameters in the application-local.properties files
   
3. Try connecting manually to MySQL to verify credentials:
   ```
   mysql -h localhost -P 3306 -u root -p
   ```

4. Check if databases were created:
   ```
   mysql -u root -p -e "SHOW DATABASES;"
   ```

5. Database parameters have been improved with:
   - Connection pool optimization (HikariCP)
   - Increased timeouts
   - Auto-creation of databases
   - Better error handling

### JDBC Connection Errors

If you see "Unable to open JDBC Connection" errors:

1. The connection URL has been updated with `createDatabaseIfNotExist=true` to automatically create the database
2. `allowPublicKeyRetrieval=true` has been added to prevent authentication issues
3. Check if MySQL server accepts connections from localhost

### Zookeeper/Kafka Issues

If services can't connect to Zookeeper or Kafka:

1. Verify containers are running:
   ```
   docker ps | grep zookeeper-local
   docker ps | grep kafka-local
   ```

2. Check that the ports (2181 for ZooKeeper, 9092 for Kafka) are correctly set 

3. Check the logs to see if there are specific connection errors:
   ```
   docker logs zookeeper-local
   docker logs kafka-local
   ```

### Redis Connection Issues

If you encounter problems with Redis:

1. Verify Redis is running:
   ```
   docker ps | grep redis-local
   ```

2. Test a simple connection:
   ```
   redis-cli -h localhost -p 6379 ping
   ``` 