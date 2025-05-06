Lớp `ApiResponseWrapper` là một lớp bao bọc chung (wrapper) cho các phản hồi API, giúp chuẩn hóa cấu trúc phản hồi từ các API của bạn. Lớp này sử dụng Lombok để tự động tạo ra các phương thức getter, setter, và constructor, và sử dụng Swagger để tạo tài liệu API.

### Mô tả lớp `ApiResponseWrapper`

Lớp này có ba thuộc tính chính:

- `status`: Mã trạng thái HTTP của phản hồi.
- `message`: Thông điệp kèm theo mã trạng thái.
- `data`: Dữ liệu được trả về trong phản hồi, có thể là bất kỳ kiểu nào (`<T>`).

Dưới đây là mã nguồn đầy đủ của lớp:

```java
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseWrapper<T> {
    @Schema(description = "HTTP status code of the response", example = "201")
    private int status;

    @Schema(description = "Message accompanying the status code", example = "User registered successfully")
    private String message;

    @Schema(description = "Data returned in the response")
    private T data;
}
```

### Cách sử dụng `ApiResponseWrapper`

Để sử dụng lớp `ApiResponseWrapper` trong các controller của Spring Boot, bạn có thể tạo và trả về đối tượng `ApiResponseWrapper` từ các phương thức xử lý yêu cầu. Dưới đây là một ví dụ minh họa:

#### Ví dụ Controller

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponseWrapper<User>> getUserById(@PathVariable Long id) {
        // Giả sử tìm thấy người dùng
        User user = new User(id, "John Doe", "john.doe@example.com");
        
        ApiResponseWrapper<User> response = new ApiResponseWrapper<>(
            HttpStatus.OK.value(),
            "User retrieved successfully",
            user
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
```

#### Ví dụ lớp User

```java
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
}
```

### Mô phỏng các phản hồi khác nhau

Bạn có thể sử dụng `ApiResponseWrapper` để mô phỏng các phản hồi với các mã trạng thái HTTP khác nhau. Dưới đây là một số ví dụ:

#### Phản hồi thành công (HTTP 200)

```java
@GetMapping("/success")
public ResponseEntity<ApiResponseWrapper<String>> successResponse() {
    ApiResponseWrapper<String> response = new ApiResponseWrapper<>(
        HttpStatus.OK.value(),
        "Request was successful",
        "This is a success response"
    );
    return new ResponseEntity<>(response, HttpStatus.OK);
}
```

#### Phản hồi không tìm thấy (HTTP 404)

```java
@GetMapping("/notfound")
public ResponseEntity<ApiResponseWrapper<String>> notFoundResponse() {
    ApiResponseWrapper<String> response = new ApiResponseWrapper<>(
        HttpStatus.NOT_FOUND.value(),
        "Resource not found",
        null
    );
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
}
```

#### Phản hồi lỗi máy chủ (HTTP 500)

```java
@GetMapping("/servererror")
public ResponseEntity<ApiResponseWrapper<String>> serverErrorResponse() {
    ApiResponseWrapper<String> response = new ApiResponseWrapper<>(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal server error occurred",
        null
    );
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

### Tổng kết

Lớp `ApiResponseWrapper` giúp bạn chuẩn hóa các phản hồi API, làm cho chúng dễ đọc và dễ bảo trì hơn. Việc sử dụng Lombok giúp giảm thiểu mã nguồn boilerplate, và việc tích hợp Swagger giúp bạn tự động tạo tài liệu API một cách dễ dàng. Bạn có thể sử dụng lớp này để trả về các phản hồi khác nhau từ các controller, bao gồm các trường hợp thành công và các lỗi khác nhau.