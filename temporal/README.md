# Temporal Login Workflow

This module implements a Temporal workflow for automating the login process using the Security Service API and accessing classroom data.

## Overview

The Temporal Login Workflow orchestrates the authentication process by:

1. Receiving a login request with username and password
2. Calling the Security Service's `/api/auth/login` endpoint
3. Processing the response and handling any errors
4. Returning the JWT token upon successful authentication
5. Prefetching classroom data during the login workflow execution
6. Providing functionality to query available classrooms using the authentication token

## Components

- **LoginWorkflow**: Interface defining the workflow contract
- **LoginWorkflowImpl**: Implementation of the workflow logic with retry policies
- **LoginActivities**: Interface defining login activities
- **LoginActivitiesImpl**: Implementation of activities to call security service API
- **ClassroomActivities**: Interface defining classroom activities
- **ClassroomActivitiesImpl**: Implementation of activities to call classroom service API
- **TemporalLoginController**: REST controller exposing the login workflow endpoint
- **TemporalClassroomController**: REST controller exposing the classroom workflow endpoint
- **JacksonConfig**: Configuration for Jackson JSON processing including Java 8 date/time handling

## Configuration

The following properties can be set in `application.properties`:

```properties
# Temporal server address
temporal.server.address=localhost:7233

# Temporal task queue name
temporal.task-queue=LoginTaskQueue

# Workflow execution timeout in seconds (default: 1 day)
temporal.workflow.execution.timeout=86400

# Security Service URL
security-service.url=http://localhost:8081

# Classroom Service URL
classroom-service.url=http://localhost:8082
```

## API Endpoints

### Login with Temporal

```
POST /temporal/login

Request body:
{
  "userName": "your_username",
  "password": "your_password"
}

Response:
{
  "token": "Bearer eyJjdHkiOiJKV1QiLCJlbmMiOiJBMTkyR0NNIiwiYWxnIjoiZGlyIn0...",
  "userName": "your_username",
  "roles": "USER",
  "message": "Đăng nhập thành công!",
  "error": null,
  "success": true,
  "workflowId": "login-workflow-abc123",
  "email": "user@example.com"
}
```

### Get Classrooms

After successful login, you can use the workflowId from the login response to get available classrooms:

```
GET /temporal/classrooms/{workflowId}

Response:
{
  "success": true,
  "error": null,
  "data": [
    {
      "id": 1,
      "name": "Mathematics 101",
      "code": "MATH101",
      "description": "Introduction to Mathematics",
      "teacherId": 1,
      "teacherName": "Admin User",
      "capacity": 25,
      "createdAt": [2025, 4, 17, 16, 21, 2],
      "updatedAt": [2025, 4, 17, 16, 21, 2]
    },
    {
      "id": 2,
      "name": "Computer Science Basics",
      "code": "CS101",
      "description": "Introduction to Computer Science",
      "teacherId": 2,
      "teacherName": "Regular User",
      "capacity": 25,
      "createdAt": [2025, 4, 17, 16, 21, 2],
      "updatedAt": [2025, 4, 17, 16, 21, 2]
    }
  ],
  "message": "Get classroom list successfully."
}
```

## Implementation Details

### Workflow Behavior

1. When a login request is received, a new workflow is created with a unique ID
2. The workflow executes the login activity to authenticate with the security service
3. If login is successful, the workflow immediately prefetches classroom data and caches it
4. The login response with token and workflow ID is returned to the client
5. The client can then use the workflow ID to query for classroom data at any time
6. The query method returns the cached classroom data without executing activities

### JSON Date/Time Handling

The application uses a custom Jackson configuration to properly handle Java 8 date/time types:

- `JacksonConfig` registers the `JavaTimeModule` module from the `jackson-datatype-jsr310` library
- Date/time arrays from the API (e.g., `[2025, 4, 17, 16, 21, 2]`) are handled as `int[]` in the DTOs

### Workflow Execution and History

- Workflows are configured with a default execution timeout of 1 day
- Workflow history retention is configured at the Temporal server level (default is 3 days)
- During this retention period, you can query workflow data even after workflow completion
- After the retention period expires, queries will return a "workflow not found" error

### Temporal Server Configuration

To change the workflow history retention period, you need to configure it at the Temporal server level:

```bash
# Example: Set retention to 7 days
docker run -d --name temporal \
  -p 7233:7233 \
  -e DYNAMIC_CONFIG_VALUE_WORKFLOW_RETENTION_TTL=604800 \
  temporalio/auto-setup:1.19.1
```

## Error Handling

The application handles various error scenarios:

1. Authentication failure: Returns appropriate error message from security service
2. Classroom API failure: Returns error with details while preserving the login token
3. Workflow not found: Returns a 404 status with message about expired session
4. Query execution errors: Returns a 500 status with details about the query failure

## Running the Application

1. Start the Temporal server (using Docker):

```bash
docker run -d --name temporal -p 7233:7233 temporalio/auto-setup:1.19.1
```

2. Start the Temporal UI (optional):

```bash
docker run -d --name temporal-ui -p 8088:8080 -e TEMPORAL_ADDRESS=host.docker.internal:7233 temporalio/ui:2.10.3
```

3. Build and run the application:

```bash
mvn clean package
java -jar target/temporal-0.0.1-SNAPSHOT.jar
```

The service will be available at http://localhost:8084. 