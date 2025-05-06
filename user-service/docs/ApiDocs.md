### Giới thiệu về Swagger Docs với Example Response

Để làm cho tài liệu API trở nên trực quan và dễ hiểu hơn, bạn có thể sử dụng annotation `@ExampleObject` của Swagger để cung cấp các ví dụ cụ thể về dữ liệu trả về dưới dạng JSON. Điều này giúp người dùng API hiểu rõ hơn về cấu trúc và nội dung của các phản hồi mà API của bạn sẽ trả về.

### Cài đặt và cấu hình Swagger với Spring Boot

#### 1. Thêm dependencies vào dự án

Đảm bảo bạn đã thêm các dependencies cần thiết vào file `build.gradle` của bạn:

```groovy
dependencies {
    implementation 'org.springdoc:springdoc-openapi-ui:1.7.0'
    implementation 'org.springdoc:springdoc-openapi-data-rest:1.7.0'
    implementation 'org.springdoc:springdoc-openapi-security:1.7.0'
}
```

#### 2. Cấu hình Swagger

Springdoc OpenAPI tự động quét các lớp controller và tạo tài liệu API từ các annotation của Spring. Bạn có thể cấu hình Swagger bằng cách thêm các thuộc tính vào file `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Ví dụ chi tiết về API với Example Response

Dưới đây là ví dụ hoàn chỉnh về cách tạo tài liệu API với Swagger, bao gồm dữ liệu mẫu cho request và response dưới dạng JSON.

#### Model Class

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin sản phẩm")
public class Product {

    @Schema(description = "ID của sản phẩm", example = "1")
    private Long id;

    @Schema(description = "Tên của sản phẩm", example = "Điện thoại di động")
    private String name;

    @Schema(description = "Giá của sản phẩm", example = "15000")
    private Double price;

    // Getters and Setters

    public Product() {}

    public Product(Long id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters and setters
}
```

#### Controller Class

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    @Operation(summary = "Lấy thông tin sản phẩm", description = "Lấy thông tin sản phẩm theo ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Product.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "name": "Điện thoại di động",
                      "price": 15000.0
                    }
                """)
            )),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @GetMapping("/products/{id}")
    public Product getProductById(@PathVariable Long id) {
        return new Product(id, "Điện thoại di động", 15000.0);  // Ví dụ dữ liệu mẫu
    }
}
```

### Cách xem tài liệu API

Khi bạn chạy ứng dụng Spring Boot, Swagger UI sẽ có sẵn tại `http://localhost:8080/swagger-ui.html`. Bạn có thể truy cập vào đây để xem tài liệu API, kiểm tra các endpoint và thử nghiệm các API trực tiếp từ giao diện web.

### Tổng kết

Swagger (OpenAPI) giúp tạo tài liệu API tự động và tương tác. Bằng cách sử dụng các annotation của Swagger trong Spring Boot, bạn có thể dễ dàng tạo ra các tài liệu API chi tiết và dễ hiểu, bao gồm cả việc tạo dữ liệu mẫu cho các endpoint. Điều này giúp các nhà phát triển khác hiểu rõ và sử dụng API của bạn một cách hiệu quả.

Cấu hình trên giúp bạn định nghĩa các ví dụ cụ thể cho response dưới dạng JSON, giúp người dùng API dễ dàng hình dung về cấu trúc và nội dung của các phản hồi mà API của bạn sẽ trả về.