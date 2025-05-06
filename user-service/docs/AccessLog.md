**CommonsRequestLoggingFilter** là một lớp trong Spring Framework cung cấp khả năng ghi lại thông tin chi tiết về các yêu cầu HTTP đến ứng dụng. Nó rất hữu ích để ghi lại các thông tin như URL, phương thức HTTP, header, payload, và các thông tin khác của các yêu cầu HTTP.

### 1. Cấu hình **CommonsRequestLoggingFilter**

Trước tiên, bạn cần tạo một bean cho **CommonsRequestLoggingFilter** trong cấu hình Spring của bạn. Dưới đây là một ví dụ cấu hình:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }
}
```

Trong ví dụ này:

- `setIncludeClientInfo(true)` ghi lại thông tin về client (IP, session).
- `setIncludeQueryString(true)` ghi lại chuỗi truy vấn.
- `setIncludeHeaders(true)` ghi lại các header của yêu cầu.
- `setIncludePayload(true)` ghi lại payload của yêu cầu.
- `setMaxPayloadLength(10000)` giới hạn chiều dài payload ghi lại.
- `setAfterMessagePrefix("REQUEST DATA : ")` đặt tiền tố cho các thông điệp log.

### 2. Thiết lập ghi log ra một file `access_log` riêng

Để ghi log ra một file riêng biệt, bạn cần cấu hình logging trong Spring Boot để chuyển các log từ **CommonsRequestLoggingFilter** vào một file riêng. Bạn có thể thực hiện điều này bằng cách cấu hình file `application.yml` như sau:

```yaml
logging:
  file:
    access_log: logs/access_log.log
  level:
    org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG
  appender:
    name: ACCESS_LOG
    type: File
    file: logs/access_log.log
  logger:
    - name: org.springframework.web.filter.CommonsRequestLoggingFilter
      level: DEBUG
      additivity: false
      appenderRef:
        - ref: ACCESS_LOG
```

Cấu hình này sẽ giúp chuyển các log từ **CommonsRequestLoggingFilter** vào file `logs/access_log.log`.

### Tổng kết

- Tạo một bean **CommonsRequestLoggingFilter** để ghi lại thông tin chi tiết của các yêu cầu HTTP.
- Cấu hình logging trong `application.yml` để ghi log vào một file riêng `access_log`.

