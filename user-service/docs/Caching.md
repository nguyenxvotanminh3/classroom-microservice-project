# Hướng Dẫn Sử Dụng Spring Boot 3.x Cache với Redis

## 1. Giới Thiệu
Spring Boot cung cấp các tính năng mạnh mẽ để sử dụng bộ nhớ đệm (cache) nhằm tăng hiệu suất ứng dụng. Redis là một lựa chọn phổ biến cho việc này vì hiệu suất cao và tính năng linh hoạt của nó. Hướng dẫn này sẽ chỉ cho bạn cách cấu hình và sử dụng bộ nhớ đệm với Redis trong Spring Boot 3.x.

## 2. Cấu Hình Dự Án
### 2.1 Thêm Dependencies
Đầu tiên, thêm các dependencies cần thiết vào tệp `build.gradle` nếu bạn sử dụng Gradle:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### 2.2 Cấu Hình Redis
Trong tệp `application.properties` hoặc `application.yml`, cấu hình kết nối tới Redis:

**application.properties**
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=yourpassword # nếu có
```

**application.yml**
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
    password: yourpassword # nếu có
```

## 3. Sử Dụng Cache trong Spring Boot

### 3.1 Cấu Hình Cache
Tạo một lớp cấu hình để cấu hình Redis Cache:

```java
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // TTL: 10 phút
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
```

### 3.2 Sử Dụng Cache trong Dịch Vụ
Sử dụng annotation `@Cacheable`, `@CachePut`, và `@CacheEvict` để cấu hình cache trong các phương thức dịch vụ:

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Cacheable(value = "users", key = "#userId")
    public User getUserById(Long userId) {
        // Giả sử phương thức này gọi một repository hoặc API để lấy dữ liệu
        return userRepository.findById(userId).orElse(null);
    }

    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        // Giả sử phương thức này cập nhật người dùng và lưu lại trong database
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        // Giả sử phương thức này xóa người dùng trong database
        userRepository.deleteById(userId);
    }
}
```

## 4. TTL (Time To Live)
TTL là thời gian sống của một mục trong bộ nhớ đệm. Trong cấu hình ở trên, TTL được thiết lập là 10 phút:

```java
.entryTtl(Duration.ofMinutes(10))
```

Bạn có thể điều chỉnh giá trị TTL theo nhu cầu của bạn. Ví dụ, bạn có thể thiết lập TTL là 1 giờ:

```java
.entryTtl(Duration.ofHours(1))
```

## 5. Kết Luận
Việc sử dụng Spring Boot 3.x cùng với Redis để quản lý cache sẽ giúp tăng hiệu suất và giảm tải cho hệ thống của bạn. Bằng cách cấu hình đúng cách và sử dụng các annotation của Spring, bạn có thể dễ dàng quản lý cache một cách hiệu quả.