**Resilience4j** là một thư viện giúp tăng cường khả năng chịu lỗi cho các ứng dụng Java, đặc biệt là trong các hệ thống phân tán. Thư viện này cung cấp các mô-đun để quản lý các lỗi phổ biến như quá tải, mất kết nối, và sự cố tạm thời. Các thành phần chính của Resilience4j bao gồm:

1. **CircuitBreaker** (Mạch ngắt)
2. **Retry** (Thử lại)
3. **RateLimiter** (Giới hạn tốc độ)
4. **Bulkhead** (Chia sẻ tài nguyên)
5. **TimeLimiter** (Giới hạn thời gian)
6. **Cache** (Bộ nhớ đệm)

Dưới đây là mô tả và ví dụ minh họa cho từng thành phần:

### 1. CircuitBreaker (Mạch ngắt)
Mạch ngắt giúp ngăn chặn các lỗi tầng dưới lan truyền và gây quá tải cho hệ thống.

**Cấu hình CircuitBreaker bằng YAML:**
```yaml
resilience4j.circuitbreaker:
  instances:
    backendA:
      registerHealthIndicator: true
      slidingWindowSize: 100
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      permittedNumberOfCallsInHalfOpenState: 10
```

**Ví dụ sử dụng CircuitBreaker:**
```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)
    .waitDurationInOpenState(Duration.ofMillis(10000))
    .build();

CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
CircuitBreaker circuitBreaker = registry.circuitBreaker("backendA");

Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
```

### 2. Retry (Thử lại)
Retry giúp thực hiện lại các thao tác khi gặp lỗi tạm thời.

**Cấu hình Retry bằng YAML:**
```yaml
resilience4j.retry:
  instances:
    backendA:
      maxAttempts: 3
      waitDuration: 5000
```

**Ví dụ sử dụng Retry:**
```java
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;

RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(5000))
    .build();

RetryRegistry registry = RetryRegistry.of(config);
Retry retry = registry.retry("backendA");

Supplier<String> supplier = Retry.decorateSupplier(retry, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
```

### 3. RateLimiter (Giới hạn tốc độ)
RateLimiter giúp giới hạn số lượng yêu cầu được thực hiện trong một khoảng thời gian nhất định.

**Cấu hình RateLimiter bằng YAML:**
```yaml
resilience4j.ratelimiter:
  instances:
    backendA:
      limitForPeriod: 10
      limitRefreshPeriod: 5000
```

**Ví dụ sử dụng RateLimiter:**
```java
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

RateLimiterConfig config = RateLimiterConfig.custom()
    .limitForPeriod(10)
    .limitRefreshPeriod(Duration.ofMillis(5000))
    .build();

RateLimiterRegistry registry = RateLimiterRegistry.of(config);
RateLimiter rateLimiter = registry.rateLimiter("backendA");

Supplier<String> supplier = RateLimiter.decorateSupplier(rateLimiter, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
```

### 4. Bulkhead (Chia sẻ tài nguyên)
Bulkhead giúp giới hạn số lượng yêu cầu đồng thời để bảo vệ tài nguyên.

**Cấu hình Bulkhead bằng YAML:**
```yaml
resilience4j.bulkhead:
  instances:
    backendA:
      maxConcurrentCalls: 5
      maxWaitDuration: 5000
```

**Ví dụ sử dụng Bulkhead:**
```java
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;

BulkheadConfig config = BulkheadConfig.custom()
    .maxConcurrentCalls(5)
    .maxWaitDuration(Duration.ofMillis(5000))
    .build();

BulkheadRegistry registry = BulkheadRegistry.of(config);
Bulkhead bulkhead = registry.bulkhead("backendA");

Supplier<String> supplier = Bulkhead.decorateSupplier(bulkhead, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
```

### 5. TimeLimiter (Giới hạn thời gian)
TimeLimiter giúp giới hạn thời gian thực hiện của một thao tác không đồng bộ.

**Cấu hình TimeLimiter bằng YAML:**
```yaml
resilience4j.timelimiter:
  instances:
    backendA:
      timeoutDuration: 2000
```

**Ví dụ sử dụng TimeLimiter:**
```java
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

TimeLimiterConfig config = TimeLimiterConfig.custom()
    .timeoutDuration(Duration.ofMillis(2000))
    .build();

TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
TimeLimiter timeLimiter = registry.timeLimiter("backendA");

Supplier<CompletionStage<String>> supplier = () -> CompletableFuture.supplyAsync(this::someMethod);
Supplier<CompletionStage<String>> decoratedSupplier = TimeLimiter.decorateCompletionStage(timeLimiter, supplier);

Try<CompletionStage<String>> result = Try.ofSupplier(decoratedSupplier);
```

### 6. Cache (Bộ nhớ đệm)
Cache giúp lưu trữ tạm thời các kết quả của các thao tác để cải thiện hiệu suất.

**Ví dụ sử dụng Cache:**
```java
import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.cache.CacheConfig;
import io.github.resilience4j.cache.CacheRegistry;

CacheConfig<String, String> config = CacheConfig.custom().ttl(Duration.ofMinutes(1)).build();
CacheRegistry<String, String> registry = CacheRegistry.of(config);
Cache<String, String> cache = registry.cache("backendA");

Function<String, String> cachedFunction = Cache.decorateFunction(cache, this::someMethod);
```

Resilience4j cung cấp các giải pháp mạnh mẽ và linh hoạt để cải thiện độ tin cậy và khả năng chịu lỗi của hệ thống, giúp bảo vệ ứng dụng khỏi các sự cố không mong muốn và cải thiện trải nghiệm người dùng.


Dưới đây là các ví dụ sử dụng annotation của Resilience4j để áp dụng các mô hình chịu lỗi như CircuitBreaker, Retry, RateLimiter, Bulkhead và TimeLimiter trong ứng dụng Spring Boot.

### 1. CircuitBreaker (Mạch ngắt)

**Cấu hình CircuitBreaker bằng YAML:**
```yaml
resilience4j.circuitbreaker:
  instances:
    backendA:
      registerHealthIndicator: true
      slidingWindowSize: 100
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      permittedNumberOfCallsInHalfOpenState: 10
```

**Ví dụ sử dụng annotation CircuitBreaker:**
```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @CircuitBreaker(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
```

### 2. Retry (Thử lại)

**Cấu hình Retry bằng YAML:**
```yaml
resilience4j.retry:
  instances:
    backendA:
      maxAttempts: 3
      waitDuration: 5000
```

**Ví dụ sử dụng annotation Retry:**
```java
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @Retry(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
```

### 3. RateLimiter (Giới hạn tốc độ)

**Cấu hình RateLimiter bằng YAML:**
```yaml
resilience4j.ratelimiter:
  instances:
    backendA:
      limitForPeriod: 10
      limitRefreshPeriod: 5000
```

**Ví dụ sử dụng annotation RateLimiter:**
```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @RateLimiter(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
```

### 4. Bulkhead (Chia sẻ tài nguyên)

**Cấu hình Bulkhead bằng YAML:**
```yaml
resilience4j.bulkhead:
  instances:
    backendA:
      maxConcurrentCalls: 5
      maxWaitDuration: 5000
```

**Ví dụ sử dụng annotation Bulkhead:**
```java
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @Bulkhead(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
```

### 5. TimeLimiter (Giới hạn thời gian)

**Cấu hình TimeLimiter bằng YAML:**
```yaml
resilience4j.timelimiter:
  instances:
    backendA:
      timeoutDuration: 2000
```

**Ví dụ sử dụng annotation TimeLimiter:**
```java
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class BackendService {

    @TimeLimiter(name = "backendA", fallbackMethod = "fallbackMethod")
    public CompletableFuture<String> someMethod() {
        return CompletableFuture.supplyAsync(() -> {
            // Logic xử lý ở đây
            return "Success";
        });
    }

    public CompletableFuture<String> fallbackMethod(Throwable t) {
        return CompletableFuture.completedFuture("Fallback response due to: " + t.getMessage());
    }
}
```

### Tích hợp Spring Boot

Để sử dụng các annotation của Resilience4j trong Spring Boot, bạn cần thêm các dependency tương ứng trong `build.gradle` của dự án:

```groovy
dependencies {
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.0.2'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
}
```

Và kích hoạt các annotation của Resilience4j trong lớp cấu hình chính:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

@SpringBootApplication
@EnableCircuitBreaker
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Những ví dụ trên sẽ giúp bạn sử dụng Resilience4j một cách hiệu quả trong các ứng dụng Spring Boot, đảm bảo hệ thống của bạn có thể chịu đựng được các sự cố tạm thời và cải thiện trải nghiệm người dùng.