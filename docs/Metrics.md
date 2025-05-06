Trong Spring Boot 3.x, bạn có thể sử dụng Micrometer để thu thập và xuất các metrics. Micrometer là một thư viện tiện ích giúp tích hợp các metrics vào các hệ thống giám sát và cảnh báo như Prometheus, Grafana, Datadog, v.v.

### 1. Thiết lập Micrometer trong Spring Boot 3.x

Đầu tiên, bạn cần thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    implementation 'io.micrometer:micrometer-core'
    implementation 'io.micrometer:micrometer-registry-prometheus' // hoặc registry khác như datadog, newrelic, etc.
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

Sau khi thêm dependencies, bạn có thể cấu hình Actuator để xuất các metrics. Thêm các cấu hình sau vào file `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    prometheus:
      enabled: true
```

### 2. Các loại Metrics trong Micrometer

#### a. Counter

Counter là một bộ đếm đơn giản chỉ tăng dần. Thích hợp cho việc đếm số lượng yêu cầu, lỗi, hoặc các sự kiện khác.

Ví dụ:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final Counter myCounter;

    public MyService(MeterRegistry registry) {
        myCounter = Counter.builder("my.counter")
                .description("A simple counter")
                .register(registry);
    }

    public void process() {
        // Do something
        myCounter.increment();
    }
}
```

#### b. Gauge

Gauge là một giá trị có thể tăng hoặc giảm. Thường dùng để theo dõi các số liệu như số lượng kết nối hiện tại, số lượng hàng đợi, hoặc dung lượng bộ nhớ.

Ví dụ:

```java
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MyService {
    private final AtomicInteger myGauge;

    public MyService(MeterRegistry registry) {
        myGauge = registry.gauge("my.gauge", new AtomicInteger(0));
    }

    public void increment() {
        myGauge.incrementAndGet();
    }

    public void decrement() {
        myGauge.decrementAndGet();
    }
}
```

#### c. Timer

Timer theo dõi thời gian và số lượng các sự kiện xảy ra, thường dùng để đo thời gian thực thi của các phương thức hoặc các khối mã.

Ví dụ:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final Timer myTimer;

    public MyService(MeterRegistry registry) {
        myTimer = Timer.builder("my.timer")
                .description("A simple timer")
                .register(registry);
    }

    public void process() {
        myTimer.record(() -> {
            // Do something
        });
    }
}
```

#### d. Summary

Summary (hoặc Distribution Summary) đo lường sự phân bố của các sự kiện, như kích thước payload của các yêu cầu hoặc thời gian thực hiện của các phương thức.

Ví dụ:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final DistributionSummary mySummary;

    public MyService(MeterRegistry registry) {
        mySummary = DistributionSummary.builder("my.summary")
                .description("A simple summary")
                .register(registry);
    }

    public void record(int value) {
        mySummary.record(value);
    }
}
```

### 3. Xuất Metrics ra hệ thống giám sát

Nếu bạn sử dụng Prometheus, bạn có thể truy cập metrics tại đường dẫn `/actuator/prometheus`. Cấu hình `application.yml` đã được cung cấp ở trên sẽ giúp bạn xuất các metrics này ra hệ thống giám sát.

### Tổng kết

- **Counter**: Đếm số lượng các sự kiện.
- **Gauge**: Theo dõi các giá trị có thể thay đổi.
- **Timer**: Theo dõi thời gian thực thi và số lượng các sự kiện.
- **Summary**: Đo lường sự phân bố của các sự kiện.

