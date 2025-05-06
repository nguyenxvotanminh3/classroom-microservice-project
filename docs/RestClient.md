Class `RestTemplateConfig` cung cấp cấu hình và tạo các bean `RestTemplate` với các kiểu xác thực khác nhau. Lớp này sử dụng OkHttpClient để quản lý các kết nối HTTP và cấu hình các interceptor để xử lý việc thêm header xác thực vào các yêu cầu HTTP.

Dưới đây là mô tả chi tiết về lớp `RestTemplateConfig` và cách sử dụng nó trong Spring Boot:

### 1. Mô tả lớp `RestTemplateConfig`

Lớp `RestTemplateConfig` cấu hình các `RestTemplate` với các kiểu xác thực khác nhau bao gồm:
- OAuth2
- Access Key
- User Token
- Bank6 Auth

#### Cấu hình OkHttpClient

Phương thức `createOkHttp3ClientHttpRequestFactory()` tạo và cấu hình một `OkHttpClient` với các thông số timeout và pool kết nối:

```java
private OkHttp3ClientHttpRequestFactory createOkHttp3ClientHttpRequestFactory() {
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MINUTES))
            .build();
    return new OkHttp3ClientHttpRequestFactory(httpClient);
}
```

### 2. Các Bean `RestTemplate`

#### a. RestTemplate with OAuth

Bean `restTemplateWithOAuth` sử dụng OAuth2 để xác thực các yêu cầu HTTP:

```java
@Bean(name = "restTemplateWithOAuth")
@Primary
public RestTemplate restTemplateWithOAuth(OAuth2AuthorizedClientManager authorizedClientManager, RestTemplateBuilder builder) {
    return builder
            .additionalInterceptors(new OAuth2ClientHttpRequestInterceptor(authorizedClientManager),
                    new DefaultHeaderInterceptor())
            .requestFactory(this::createOkHttp3ClientHttpRequestFactory)
            .build();
}
```

#### b. RestTemplate with Access Key

Bean `restTemplateWithAccessKey` sử dụng một access key cố định để xác thực các yêu cầu HTTP:

```java
@Bean(name = "restTemplateWithAccessKey")
public RestTemplate restTemplateWithAccessKey(RestTemplateBuilder builder) {
    return builder
            .additionalInterceptors(
                    (request, body, execution) -> {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer someAccessKey");
                        return execution.execute(request, body);
                    },
                    new DefaultHeaderInterceptor())
            .requestFactory(this::createOkHttp3ClientHttpRequestFactory)
            .build();
}
```

#### c. RestTemplate with User Token

Bean `restTemplateWithUserToken` sử dụng token của người dùng hiện tại để xác thực các yêu cầu HTTP:

```java
@Bean(name = "restTemplateWithUserToken")
public RestTemplate restTemplateWithUserToken(RestTemplateBuilder builder) {
    return builder
            .additionalInterceptors(
                    (request, body, execution) -> {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.getCredentials() instanceof Jwt jwt) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
                        }
                        return execution.execute(request, body);
                    },
                    new DefaultHeaderInterceptor())
            .requestFactory(this::createOkHttp3ClientHttpRequestFactory)
            .build();
}
```

#### d. RestTemplate with Bank6 Auth

Bean `restTemplateBank6` sử dụng một access key được cấu hình trước để xác thực các yêu cầu HTTP:

```java
@Bean(name = "restTemplateBank6")
public RestTemplate restTemplateBank6(RestTemplateBuilder builder) {
    return builder
            .additionalInterceptors(
                    (request, body, execution) -> {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, bank6Auth);
                        return execution.execute(request, body);
                    },
                    new DefaultHeaderInterceptor())
            .requestFactory(this::createOkHttp3ClientHttpRequestFactory)
            .build();
}
```

### 3. Sử dụng các `RestTemplate` trong ứng dụng

Dưới đây là ví dụ về cách sử dụng các `RestTemplate` đã cấu hình trong một service hoặc controller:

#### Ví dụ Service sử dụng RestTemplate with OAuth

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MyService {

    private final RestTemplate restTemplateWithOAuth;

    public MyService(@Qualifier("restTemplateWithOAuth") RestTemplate restTemplateWithOAuth) {
        this.restTemplateWithOAuth = restTemplateWithOAuth;
    }

    public String callExternalApi() {
        String url = "https://api.example.com/resource";
        return restTemplateWithOAuth.getForObject(url, String.class);
    }
}
```

#### Ví dụ Controller sử dụng RestTemplate with Access Key

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class MyController {

    private final RestTemplate restTemplateWithAccessKey;

    public MyController(@Qualifier("restTemplateWithAccessKey") RestTemplate restTemplateWithAccessKey) {
        this.restTemplateWithAccessKey = restTemplateWithAccessKey;
    }

    @GetMapping("/external")
    public ResponseEntity<String> getExternalData() {
        String url = "https://api.example.com/data";
        String response = restTemplateWithAccessKey.getForObject(url, String.class);
        return ResponseEntity.ok(response);
    }
}
```

### Tổng kết

Lớp `RestTemplateConfig` cung cấp các cấu hình chi tiết cho `RestTemplate` với các kiểu xác thực khác nhau, giúp bạn dễ dàng quản lý và sử dụng các `RestTemplate` trong ứng dụng Spring Boot của mình. Bạn có thể tạo và sử dụng các bean này trong các service hoặc controller để gọi các API bên ngoài một cách an toàn và hiệu quả.