### Apache Dubbo 3

Apache Dubbo là một framework RPC (Remote Procedure Call) mã nguồn mở cho phép các ứng dụng xây dựng hệ thống phân tán một cách dễ dàng và hiệu quả. Dubbo hỗ trợ nhiều giao thức truyền thông, cân bằng tải, và khả năng mở rộng, giúp cải thiện hiệu suất và độ tin cậy của các hệ thống phân tán.

### Tính năng chính của Apache Dubbo 3
1. **RPC Framework**: Hỗ trợ việc gọi các phương thức từ xa như gọi các phương thức cục bộ.
2. **Nhiều giao thức và cơ chế cân bằng tải**: Hỗ trợ nhiều giao thức truyền thông và cơ chế cân bằng tải khác nhau.
3. **Tích hợp tốt với Spring Boot**: Hỗ trợ tích hợp dễ dàng với Spring Boot, giúp đơn giản hóa việc cấu hình và sử dụng.
4. **Khả năng mở rộng và độ tin cậy cao**: Được thiết kế để dễ dàng mở rộng và đảm bảo độ tin cậy của hệ thống.

### Cách sử dụng Apache Dubbo 3 với Spring Boot 3.x

#### 1. Thêm các phụ thuộc vào dự án
Thêm các phụ thuộc cần thiết vào tệp `build.gradle`.

```groovy
implementation 'org.apache.dubbo:dubbo-spring-boot-starter:3.0.5'
implementation 'org.springframework.boot:spring-boot-starter'
implementation 'org.springframework.boot:spring-boot-starter-web'
```

#### 2. Cấu hình Dubbo trong `application.yml`
Cấu hình Dubbo để kết nối với các dịch vụ cung cấp và tiêu thụ.

```yaml
dubbo:
  application:
    name: dubbo-demo-provider
  registry:
    address: zookeeper://127.0.0.1:2181
  protocol:
    name: dubbo
    port: 20880
  scan:
    base-packages: com.example.dubbodemo
```

#### 3. Tạo một dịch vụ cung cấp (Provider)
Tạo một interface và triển khai nó để cung cấp dịch vụ.

```java
package com.example.dubbodemo.service;

public interface DemoService {
    String sayHello(String name);
}
```

Triển khai interface và sử dụng annotation `@Service` của Dubbo để đăng ký dịch vụ.

```java
package com.example.dubbodemo.service.impl;

import com.example.dubbodemo.service.DemoService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class DemoServiceImpl implements DemoService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
```

#### 4. Tạo một dịch vụ tiêu thụ (Consumer)
Tạo một controller để tiêu thụ dịch vụ Dubbo.

```java
package com.example.dubbodemo.controller;

import com.example.dubbodemo.service.DemoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @DubboReference
    private DemoService demoService;

    @GetMapping("/hello")
    public String sayHello(@RequestParam String name) {
        return demoService.sayHello(name);
    }
}
```

#### 5. Chạy ứng dụng Spring Boot
Chạy ứng dụng Spring Boot và kiểm tra dịch vụ Dubbo bằng cách gửi yêu cầu HTTP đến endpoint `/hello`.

### Lưu ý khi sử dụng Apache Dubbo 3 với Spring Boot 3.x
1. **Cấu hình Zookeeper**: Đảm bảo rằng Zookeeper được cấu hình và chạy để quản lý các dịch vụ Dubbo.
2. **Tích hợp Spring Boot**: Sử dụng các annotation của Dubbo (`@DubboService`, `@DubboReference`) để tích hợp dịch vụ Dubbo vào ứng dụng Spring Boot.
3. **Kiểm tra và giám sát**: Sử dụng các công cụ kiểm tra và giám sát để đảm bảo các dịch vụ Dubbo hoạt động đúng và hiệu quả.

