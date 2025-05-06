### Giới thiệu về Spring Boot Logging

Spring Boot cung cấp hỗ trợ tích hợp cho các framework logging phổ biến như Logback, Log4j2 và Java Util Logging. Mặc định, Spring Boot sử dụng Logback làm framework logging chính. Logging giúp ghi lại các sự kiện xảy ra trong ứng dụng, hỗ trợ việc phát hiện và xử lý lỗi cũng như giám sát hiệu suất.

### Cấu hình Logback với file XML

Dưới đây là cách cấu hình Logback bằng file XML, bật MDC (Mapped Diagnostic Context), và cấu hình log ghi thành nhiều file theo các appender khác nhau. Đồng thời, chúng ta cũng sẽ bật chế độ log debug cho Hibernate và Spring Security.

#### 1. Tạo file cấu hình Logback

Tạo file `logback-spring.xml` trong thư mục `src/main/resources` với nội dung sau:

```xml
<configuration>

    <!-- Định nghĩa appender để ghi log vào console -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log vào file chung -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log của Hibernate -->
    <appender name="HIBERNATE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/hibernate.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/hibernate.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log của Spring Security -->
    <appender name="SECURITY" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Bật MDC (Mapped Diagnostic Context) -->
    <turboFilter class="ch.qos.logback.classic.turbo.MDCFilter">
        <MDCKey>user</MDCKey>
    </turboFilter>

    <!-- Cấu hình root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <!-- Cấu hình logger cho Hibernate -->
    <logger name="org.hibernate" level="DEBUG" additivity="false">
        <appender-ref ref="HIBERNATE"/>
    </logger>

    <!-- Cấu hình logger cho Spring Security -->
    <logger name="org.springframework.security" level="DEBUG" additivity="false">
        <appender-ref ref="SECURITY"/>
    </logger>

</configuration>
```

#### 2. Bật MDC (Mapped Diagnostic Context)

MDC cho phép bạn thêm thông tin chẩn đoán vào các log entry. Điều này hữu ích khi bạn cần theo dõi các yêu cầu hoặc giao dịch riêng biệt trong log.

Trong file cấu hình trên, chúng ta đã thêm `turboFilter` để bật MDC:

```xml
<turboFilter class="ch.qos.logback.classic.turbo.MDCFilter">
    <MDCKey>user</MDCKey>
</turboFilter>
```

Để sử dụng MDC trong mã nguồn, bạn có thể thêm thông tin vào MDC như sau:

```java
import org.slf4j.MDC;

public class MyService {

    public void someMethod() {
        MDC.put("user", "user123");
        // Thực hiện logic của bạn
        MDC.clear();
    }
}
```

#### 3. Cấu hình log ghi thành nhiều file theo các appender khác nhau

Trong file cấu hình `logback-spring.xml`, chúng ta đã định nghĩa các appender khác nhau cho console, file chung, Hibernate và Spring Security.

#### 4. Bật log debug cho Hibernate và Spring Security

Chúng ta đã cấu hình logger cho Hibernate và Spring Security với mức độ `DEBUG` và gán các appender tương ứng:

```xml
<logger name="org.hibernate" level="DEBUG" additivity="false">
    <appender-ref ref="HIBERNATE"/>
</logger>

<logger name="org.springframework.security" level="DEBUG" additivity="false">
    <appender-ref ref="SECURITY"/>
</logger>
```

### Tổng kết

Với cấu hình trên, bạn đã thiết lập logging trong Spring Boot bằng cách sử dụng Logback và cấu hình qua file XML. Bạn đã bật MDC để thêm thông tin chẩn đoán vào các log entry, cấu hình log ghi thành nhiều file theo các appender khác nhau, và bật log debug cho Hibernate và Spring Security. Cấu hình này giúp bạn quản lý và phân tích log một cách hiệu quả, đặc biệt là trong các ứng dụng phức tạp và phân tán.


Để ghi thông tin traceId và spanId vào log bằng cách sử dụng Spring Boot, Logback và OpenTelemetry, bạn cần cấu hình Logback để ghi các giá trị này từ Mapped Diagnostic Context (MDC). OpenTelemetry tự động chèn các giá trị traceId và spanId vào MDC khi bạn sử dụng nó để theo dõi (trace) các yêu cầu và giao dịch.

Dưới đây là các bước để cấu hình Logback và sử dụng `%X` để ghi traceId và spanId vào log.

### 1. Thêm dependencies vào dự án

Đảm bảo bạn đã thêm các dependencies cần thiết vào file `build.gradle` của bạn:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-log4j2'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel:1.13.4'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp:1.24.0'
    implementation 'io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.24.0'
}
```

### 2. Cấu hình OpenTelemetry

Cấu hình OpenTelemetry trong file `application.yml` để thu thập và gửi các trace đến Jaeger:

```yaml
management:
  tracing:
    enabled: true

  metrics:
    export:
      prometheus:
        enabled: true

otel:
  traces:
    exporter: otlp
  exporter:
    otlp:
      endpoint: http://localhost:4317
  resource:
    attributes:
      service.name: my-service
```

### 3. Cấu hình Logback

Tạo file `logback-spring.xml` trong thư mục `src/main/resources` với nội dung sau để ghi các giá trị traceId và spanId từ MDC vào log:

```xml
<configuration>

    <!-- Định nghĩa appender để ghi log vào console -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg %nMDC:[traceId=%X{traceId}, spanId=%X{spanId}]%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log vào file chung -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg %nMDC:[traceId=%X{traceId}, spanId=%X{spanId}]%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log của Hibernate -->
    <appender name="HIBERNATE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/hibernate.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/hibernate.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg %nMDC:[traceId=%X{traceId}, spanId=%X{spanId}]%n</pattern>
        </encoder>
    </appender>

    <!-- Định nghĩa appender để ghi log của Spring Security -->
    <appender name="SECURITY" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg %nMDC:[traceId=%X{traceId}, spanId=%X{spanId}]%n</pattern>
        </encoder>
    </appender>

    <!-- Cấu hình root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <!-- Cấu hình logger cho Hibernate -->
    <logger name="org.hibernate" level="DEBUG" additivity="false">
        <appender-ref ref="HIBERNATE"/>
    </logger>

    <!-- Cấu hình logger cho Spring Security -->
    <logger name="org.springframework.security" level="DEBUG" additivity="false">
        <appender-ref ref="SECURITY"/>
    </logger>

</configuration>
```

### 4. Sử dụng OpenTelemetry trong mã nguồn

OpenTelemetry tự động chèn các giá trị traceId và spanId vào MDC khi bạn sử dụng nó để theo dõi các yêu cầu và giao dịch. Bạn có thể thêm các span thủ công vào ứng dụng của mình để theo dõi các phần cụ thể của mã.

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    @Autowired
    private Tracer tracer;

    public void myMethod() {
        Span newSpan = tracer.nextSpan().name("myMethodSpan").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan.start())) {
            // Thực hiện logic của bạn ở đây
            performTask();
        } finally {
            newSpan.end();
        }
    }

    private void performTask() {
        // Logic của task
    }
}
```

### Tổng kết

Với cấu hình trên, bạn đã thiết lập logging trong Spring Boot bằng cách sử dụng Logback và cấu hình qua file XML. Bạn đã bật MDC để thêm thông tin traceId và spanId vào các log entry. Cấu hình này giúp bạn quản lý và phân tích log một cách hiệu quả, đặc biệt là trong các ứng dụng phức tạp và phân tán. Tracing với OpenTelemetry và logging với Logback cung cấp một cái nhìn sâu sắc về hiệu suất và hoạt động của hệ thống.