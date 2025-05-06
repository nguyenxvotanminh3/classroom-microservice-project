### Giới thiệu về Spring Boot Security

Spring Boot Security là một phần của Spring Framework, cung cấp các tính năng bảo mật mạnh mẽ cho các ứng dụng Spring Boot. Nó giúp bảo vệ ứng dụng của bạn khỏi các cuộc tấn công như CSRF, XSS, và đảm bảo rằng chỉ những người dùng được ủy quyền mới có thể truy cập vào các tài nguyên cụ thể.

### Giới thiệu về Keycloak

Keycloak là một giải pháp mã nguồn mở để quản lý nhận diện và truy cập. Nó cung cấp các tính năng như đăng nhập một lần (Single Sign-On), xác thực đa yếu tố (Multi-Factor Authentication), quản lý người dùng và các quyền truy cập. Keycloak hỗ trợ nhiều giao thức bảo mật như OAuth2, OpenID Connect và SAML.

### Tích hợp Spring Boot với Keycloak sử dụng OAuth2

#### 1. Thêm dependencies vào dự án

Thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.keycloak:keycloak-spring-boot-starter:21.0.1'
}
```

#### 2. Cấu hình Spring Security để sử dụng OAuth2

Cấu hình Spring Security để sử dụng Keycloak làm nhà cung cấp OAuth2:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: my-client
            client-secret: my-secret
            scope: openid, profile, email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/keycloak"
        provider:
          keycloak:
            issuer-uri: http://localhost:8080/realms/myrealm
```

#### 3. Cấu hình bảo mật Spring Security

Tạo một lớp cấu hình bảo mật để thiết lập các quy tắc bảo mật cho ứng dụng:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:8080");

        http
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers("/public/**").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/oauth2/authorization/keycloak")
            )
            .logout(logout ->
                logout
                    .logoutSuccessHandler(oidcLogoutSuccessHandler)
            )
            .oauth2Client()
            .oauth2ResourceServer().jwt();

        return http.build();
    }
}
```

### Giới thiệu về Enforcer Policy trong Keycloak

Enforcer Policy trong Keycloak giúp bảo vệ các tài nguyên của bạn bằng cách sử dụng các chính sách bảo mật và quyền truy cập được cấu hình trong Keycloak. Nó hỗ trợ các chính sách dựa trên vai trò, nhóm và các điều kiện khác.

### Tích hợp Enforcer Policy với Spring Boot

#### 1. Cấu hình Enforcer Policy trong Keycloak

Để cấu hình Enforcer Policy trong Keycloak, bạn cần thực hiện các bước sau trong giao diện quản trị Keycloak:

1. Tạo một Client trong Keycloak và kích hoạt "Authorization Enabled".
2. Định nghĩa các Resource, Scope và Policy trong tab "Authorization" của Client.

#### 2. Cấu hình Spring Boot để sử dụng Enforcer Policy

Cấu hình Spring Boot để sử dụng Enforcer Policy với Keycloak:

```yaml
keycloak:
  auth-server-url: http://localhost:8080/auth
  realm: myrealm
  resource: my-client
  credentials:
    secret: my-secret
  policy-enforcer-config:
    enforcement-mode: ENFORCING
```

#### 3. Sử dụng Enforcer Policy trong mã nguồn

Tạo một lớp cấu hình bảo mật để sử dụng Enforcer Policy trong Spring Boot:

```java
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public KeycloakConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers("/public/**").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/oauth2/authorization/keycloak")
            )
            .oauth2ResourceServer().jwt();

        return http.build();
    }
}
```

### Tổng kết

Spring Boot Security cung cấp các tính năng bảo mật mạnh mẽ cho các ứng dụng Spring Boot. Bằng cách tích hợp với Keycloak sử dụng OAuth2, bạn có thể dễ dàng quản lý xác thực và ủy quyền trong hệ thống của mình. Keycloak Enforcer Policy cung cấp một cách linh hoạt để bảo vệ các tài nguyên của bạn dựa trên các chính sách bảo mật được cấu hình trong Keycloak. Việc tích hợp này giúp bạn xây dựng các ứng dụng bảo mật và dễ dàng quản lý quyền truy cập của người dùng.

### Giới thiệu cách cấu hình bảo vệ một endpoint bằng cách sử dụng Keycloak Authorization

Keycloak cung cấp một cách mạnh mẽ và linh hoạt để bảo vệ các endpoint trong ứng dụng của bạn thông qua các chính sách ủy quyền (Authorization Policies). Khi tích hợp với Spring Boot, bạn có thể bảo vệ các endpoint của mình bằng cách sử dụng Keycloak Authorization Services.

Dưới đây là các bước để cấu hình bảo vệ một endpoint sử dụng Keycloak Authorization:

### 1. Cấu hình Keycloak

#### a. Tạo Realm và Client

1. Đăng nhập vào Keycloak Admin Console.
2. Tạo một Realm mới hoặc sử dụng một Realm hiện có.
3. Trong Realm, tạo một Client mới:
    - Client ID: `my-client`
    - Client Protocol: `openid-connect`
    - Root URL: URL của ứng dụng Spring Boot của bạn (ví dụ: `http://localhost:8080`)

#### b. Kích hoạt Authorization

1. Trong trang cấu hình của Client, bật `Authorization Enabled`.
2. Điều này sẽ thêm một tab `Authorization` vào trang cấu hình của Client.

#### c. Định nghĩa Resource và Scope

1. Trong tab `Authorization`, chuyển đến `Resources` và tạo một Resource mới:
    - Name: `my-resource`
    - URI: `/protected-endpoint`

2. Thêm các Scope tương ứng nếu cần (ví dụ: `view`, `edit`).

#### d. Định nghĩa Policies và Permissions

1. Trong tab `Authorization`, chuyển đến `Policies` và tạo các Policy để xác định quyền truy cập (ví dụ: dựa trên vai trò người dùng).
2. Trong tab `Authorization`, chuyển đến `Permissions` và tạo các Permission để liên kết Resource, Scope và Policies.

### 2. Cấu hình Spring Boot

#### a. Thêm Dependencies

Thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.keycloak:keycloak-spring-boot-starter:21.0.1'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.keycloak:keycloak-authz-client:21.0.1'
}
```

#### b. Cấu hình Keycloak trong `application.yml`

Thêm cấu hình Keycloak vào file `application.yml`:

```yaml
keycloak:
  auth-server-url: http://localhost:8080/auth
  realm: myrealm
  resource: my-client
  credentials:
    secret: my-secret
  policy-enforcer-config:
    enforcement-mode: ENFORCING

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/auth/realms/myrealm
```

#### c. Cấu hình Spring Security

Tạo một lớp cấu hình bảo mật để bảo vệ các endpoint bằng Keycloak Authorization:

```java
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@KeycloakConfiguration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers("/public/**").permitAll()
                    .antMatchers("/protected-endpoint/**").hasRole("USER")
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer()
            .jwt();

        return http.build();
    }
}
```

#### d. Bảo vệ Endpoint

Đảm bảo rằng endpoint của bạn được bảo vệ bằng cách chỉ cho phép người dùng có quyền truy cập hợp lệ:

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/protected-endpoint")
public class ProtectedController {

    @GetMapping
    public String protectedResource() {
        return "This is a protected resource";
    }
}
```

### Tổng kết

Keycloak Authorization cung cấp một cách mạnh mẽ và linh hoạt để bảo vệ các endpoint trong ứng dụng của bạn. Bằng cách sử dụng các chính sách ủy quyền trong Keycloak và tích hợp với Spring Boot Security, bạn có thể dễ dàng kiểm soát quyền truy cập đến các tài nguyên của mình. Cấu hình này giúp bạn đảm bảo rằng chỉ những người dùng được ủy quyền mới có thể truy cập vào các endpoint được bảo vệ trong ứng dụng của bạn.