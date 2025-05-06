# Security Service Changes for Email in Login Response

## 1. Update LoginResponse DTO

Find the `LoginResponse` or similar class in the security service and add the email field:

```java
public class LoginResponse {
    private String token;
    private String userName;
    private String roles;
    private String message;
    private String error;
    private boolean success;
    // Add this field:
    private String email;
    
    // Update constructor, getter/setter or builder
}
```

## 2. Update Login Service/Controller

Find the login service method that handles authentication and modify it to include the user's email:

```java
public LoginResponse authenticate(LoginRequest request) {
    // Existing authentication logic...
    
    // After successful authentication:
    User user = userRepository.findByUsername(request.getUserName());
    
    // When building the response, include the email
    return LoginResponse.builder()
            .token(token)
            .userName(user.getUsername())
            .roles(user.getRoles())
            .email(user.getEmail())  // Add this line
            .message("Login successful")
            .success(true)
            .build();
}
```

## 3. Check JWT Token Generation

If you're including user details in the JWT token, you might want to add the email there as well:

```java
private String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("username", user.getUsername());
    claims.put("roles", user.getRoles());
    claims.put("email", user.getEmail());  // Add this line
    
    // Generate and return token
}
```

## 4. Test the Changes

After implementing these changes, test the login API using a tool like Postman or curl:

```
POST /api/auth/login
{
    "userName": "test_user",
    "password": "password"
}
```

The response should now include the email field:

```json
{
    "token": "Bearer eyJhbGciOiJIUzI1NiJ9...",
    "userName": "test_user",
    "roles": "USER",
    "email": "user@example.com",
    "message": "Login successful!",
    "success": true
}
``` 