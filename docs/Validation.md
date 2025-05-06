### Giới thiệu về Validation sử dụng Jakarta Bean Validation

Jakarta Bean Validation (trước đây là Java EE Bean Validation và sau đó là Hibernate Validator) là một chuẩn của Java cho phép bạn xác thực (validate) các đối tượng Java bằng cách sử dụng các annotation. Jakarta Bean Validation cung cấp một cách tiện lợi và dễ dàng để đảm bảo rằng các thuộc tính của các đối tượng tuân theo các quy tắc nhất định.

### Các loại Validation và ý nghĩa

Jakarta Bean Validation cung cấp một số các annotation để xác thực các thuộc tính của đối tượng. Dưới đây là một số các annotation phổ biến:

1. **@NotNull**: Đảm bảo rằng giá trị không được null.
2. **@Size**: Đảm bảo rằng kích thước của một tập hợp (collection), mảng, map hoặc chuỗi (string) nằm trong một phạm vi nhất định.
3. **@Min**: Đảm bảo rằng giá trị của một số (number) không nhỏ hơn giá trị tối thiểu đã định.
4. **@Max**: Đảm bảo rằng giá trị của một số (number) không lớn hơn giá trị tối đa đã định.
5. **@Pattern**: Đảm bảo rằng một chuỗi (string) phù hợp với một biểu thức chính quy (regular expression).
6. **@Email**: Đảm bảo rằng một chuỗi (string) có định dạng email hợp lệ.
7. **@Past**: Đảm bảo rằng một ngày (date) nằm trong quá khứ.
8. **@Future**: Đảm bảo rằng một ngày (date) nằm trong tương lai.
9. **@Positive**: Đảm bảo rằng một số (number) là số dương.
10. **@Negative**: Đảm bảo rằng một số (number) là số âm.
11. **@AssertTrue**: Đảm bảo rằng giá trị của thuộc tính là true.
12. **@AssertFalse**: Đảm bảo rằng giá trị của thuộc tính là false.

### Cách sử dụng Jakarta Bean Validation

#### 1. Thêm dependencies vào dự án

Để sử dụng Jakarta Bean Validation trong một dự án Spring Boot, bạn cần thêm dependencies vào file `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

#### 2. Sử dụng các annotation để xác thực các thuộc tính của đối tượng

Ví dụ, bạn có một lớp `User` và bạn muốn xác thực các thuộc tính của nó:

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class User {

    @NotNull(message = "Name cannot be null")
    @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
    private String name;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    // Getters and setters
}
```

#### 3. Kích hoạt validation trong controller

Để kích hoạt validation trong controller của Spring Boot, bạn sử dụng annotation `@Valid` hoặc `@Validated` trong các phương thức của controller:

```java
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody User user) {
        // Nếu validation thất bại, một MethodArgumentNotValidException sẽ được ném ra
        return new ResponseEntity<>("User is valid", HttpStatus.OK);
    }
}
```

#### 4. Xử lý lỗi validation

Khi validation thất bại, Spring Boot sẽ ném ra một ngoại lệ như `MethodArgumentNotValidException`. Bạn có thể xử lý ngoại lệ này bằng cách sử dụng một `@ControllerAdvice`:

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
```

### Tổng kết

Jakarta Bean Validation cung cấp một cách tiện lợi và mạnh mẽ để xác thực các thuộc tính của đối tượng trong Java. Bằng cách sử dụng các annotation xác thực, bạn có thể đảm bảo rằng dữ liệu của bạn luôn tuân theo các quy tắc đã định, giúp giảm thiểu lỗi và cải thiện tính nhất quán của ứng dụng. Trong Spring Boot, việc tích hợp Jakarta Bean Validation rất dễ dàng và linh hoạt, cho phép bạn xác thực dữ liệu ở nhiều lớp khác nhau trong ứng dụng của mình.