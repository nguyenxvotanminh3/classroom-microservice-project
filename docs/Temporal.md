### Giới thiệu về Temporal Workflow

Temporal là một nền tảng nguồn mở mạnh mẽ giúp bạn xây dựng các ứng dụng phân tán và đáng tin cậy bằng cách quản lý trạng thái và điều hành workflow. Temporal hỗ trợ việc xây dựng các workflow có thể mở rộng, khôi phục từ lỗi, và dễ dàng duy trì, đặc biệt là trong các hệ thống phức tạp.

### Tính năng chính của Temporal

- **Quản lý trạng thái**: Temporal duy trì trạng thái của các workflow và hoạt động của bạn một cách bền bỉ.
- **Khôi phục từ lỗi**: Temporal tự động khôi phục workflow từ các lỗi mà không mất trạng thái.
- **Lập lịch và điều phối**: Temporal cung cấp cơ chế lập lịch và điều phối các hoạt động phức tạp.
- **Khả năng mở rộng**: Temporal được thiết kế để dễ dàng mở rộng để xử lý số lượng lớn workflow và hoạt động.

### 1. Thêm dependencies vào dự án

Trước tiên, bạn cần thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'io.temporal:temporal-sdk:1.13.0'
    implementation 'io.temporal:temporal-spring-boot-starter:1.13.0'
}
```

### 2. Cấu hình Temporal

Tạo một file cấu hình cho Temporal trong `application.yml`:

```yaml
temporal:
  server:
    host: "localhost:7233"  # Địa chỉ của Temporal service
  workers:
    default:
      task-queue: "MyTaskQueue"
```

### 3. Định nghĩa Workflow Interface

Tạo một interface cho workflow của bạn. Interface này sẽ chứa định nghĩa của các phương thức workflow:

```java
package com.example.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {

    @WorkflowMethod
    String executeWorkflow(String input);
}
```

### 4. Thực hiện Workflow

Tạo một lớp triển khai interface workflow. Lớp này sẽ chứa logic thực hiện của workflow:

```java
package com.example.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

    private final MyActivities activities = Workflow.newActivityStub(MyActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String executeWorkflow(String input) {
        // Thực hiện logic của workflow và gọi activity
        return activities.performActivity(input);
    }
}
```

### 5. Định nghĩa Activities Interface

Tạo một interface cho các activities. Các activities là các tác vụ đơn lẻ thực hiện trong workflow:

```java
package com.example.temporal.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MyActivities {

    @ActivityMethod
    String performActivity(String input);
}
```

### 6. Thực hiện Activities

Tạo một lớp triển khai interface activities:

```java
package com.example.temporal.workflow;

public class MyActivitiesImpl implements MyActivities {

    @Override
    public String performActivity(String input) {
        // Thực hiện logic của activity
        return "Activity performed with input: " + input;
    }
}
```

### 7. Gọi Workflow từ Spring Boot

Tạo một service trong Spring Boot để gọi workflow:

```java
package com.example.temporal.service;

import com.example.temporal.workflow.MyWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowService {

    @Autowired
    private WorkflowClient workflowClient;

    public String startWorkflow(String input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("MyTaskQueue")
                .build();

        MyWorkflow workflow = workflowClient.newWorkflowStub(MyWorkflow.class, options);
        return workflow.executeWorkflow(input);
    }
}
```

### 8. Sử dụng Workflow Service trong Controller

Tạo một controller để sử dụng service và khởi chạy workflow:

```java
package com.example.temporal.controller;

import com.example.temporal.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @GetMapping("/start-workflow")
    public String startWorkflow(@RequestParam String input) {
        return workflowService.startWorkflow(input);
    }
}
```

### Tổng kết

Temporal Spring Boot Starter giúp bạn tự động đăng ký các workflow và activity mà không cần cấu hình thủ công. Bạn chỉ cần định nghĩa các interface và triển khai logic của mình. Temporal sẽ tự động quản lý việc đăng ký và khởi chạy các workflow và activity dựa trên các annotation và cấu hình mà bạn cung cấp.