### Giới thiệu Tracing trong Spring Boot 3.x với Micrometer, OpenTelemetry và Jaeger

Tracing là quá trình theo dõi và ghi lại các luồng dữ liệu hoặc các yêu cầu (requests) đi qua các dịch vụ trong hệ thống phân tán. Điều này giúp bạn có cái nhìn sâu sắc về hiệu suất của hệ thống, cũng như phát hiện và xử lý các vấn đề tiềm ẩn.

Trong Spring Boot 3.x, Micrometer cung cấp tích hợp với OpenTelemetry (OTel) để thu thập các số liệu (metrics) và tracing. Jaeger là một hệ thống giám sát mã nguồn mở, giúp bạn giám sát và phân tích các trace trong hệ thống.

### Các bước cấu hình Tracing trong Spring Boot 3.x

#### 1. Thêm dependencies vào dự án

Đầu tiên, bạn cần thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel:1.13.4'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp:1.24.0'
    implementation 'io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.24.0'
}
```

#### 2. Cấu hình OpenTelemetry và Jaeger

Bạn cần cấu hình OpenTelemetry trong file `application.yml` để thu thập và gửi các trace đến Jaeger:

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

#### 3. Cài đặt và chạy Jaeger

Bạn có thể cài đặt Jaeger bằng Docker. Dưới đây là lệnh Docker để chạy Jaeger:

```sh
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 14250:14250 \
  -p 9411:9411 \
  jaegertracing/all-in-one:1.31
```

Truy cập giao diện người dùng của Jaeger tại `http://localhost:16686`.

#### 4. Sử dụng tracing trong ứng dụng

Micrometer và Spring Boot sẽ tự động thêm các trace ID vào log và gửi các trace này đến Jaeger. Bạn có thể thêm các span thủ công vào ứng dụng của mình để theo dõi các phần cụ thể của mã.

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

#### 5. Ví dụ sử dụng Span trong Controller

Dưới đây là một ví dụ minh họa cách sử dụng Span trong một controller Spring Boot:

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    @Autowired
    private Tracer tracer;

    @GetMapping("/trace-example")
    public String traceExample() {
        Span span = tracer.nextSpan().name("traceExampleSpan").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // Thực hiện logic của bạn ở đây
            return performTask();
        } finally {
            span.end();
        }
    }

    private String performTask() {
        // Logic của task
        return "Tracing example completed.";
    }
}
```

### Tổng kết

Tracing trong Spring Boot 3.x với Micrometer và OpenTelemetry giúp bạn dễ dàng theo dõi và giám sát các luồng yêu cầu trong hệ thống phân tán của mình. Bằng cách tích hợp với Jaeger, bạn có thể thu thập và phân tích các trace để cải thiện hiệu suất và khả năng quan sát của hệ thống. Các ví dụ trên minh họa cách cấu hình và sử dụng tracing trong ứng dụng Spring Boot của bạn, bao gồm việc sử dụng các span để theo dõi các phần cụ thể của logic ứng dụng.