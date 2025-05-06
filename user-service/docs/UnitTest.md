## Hướng dẫn và Các Best Practices cho Kiểm Thử Đơn Vị trong Spring Boot

### Giới Thiệu về Kiểm Thử Đơn Vị

**Kiểm thử đơn vị (Unit Testing)** là quá trình kiểm tra các đơn vị nhỏ nhất của mã nguồn, thường là các hàm hoặc phương thức, để đảm bảo rằng chúng hoạt động đúng đắn. Trong Spring Boot, kiểm thử đơn vị là một phần quan trọng của quá trình phát triển phần mềm, giúp phát hiện sớm các lỗi và cải thiện chất lượng mã nguồn.

### Các Công Cụ Kiểm Thử Đơn Vị

1. **JUnit 5**: Là một framework kiểm thử phổ biến trong Java, được sử dụng để viết và chạy các kiểm thử đơn vị.
2. **Mockito**: Là một framework dùng để tạo mock object, giúp kiểm thử các thành phần mà không cần phụ thuộc vào các thành phần khác.
3. **Spring Boot Test**: Cung cấp các tiện ích và annotation để hỗ trợ kiểm thử trong Spring Boot.

### Hướng Dẫn Kiểm Thử Đơn Vị

#### 1. Cấu Hình Dự Án cho Kiểm Thử Đơn Vị

Thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.junit.jupiter:junit-jupiter-api'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine'
}
```

#### 2. Viết Kiểm Thử Đơn Vị cho Service

Ví dụ, giả sử bạn có một `ProductService` với phương thức `getProductById`:

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
    }
}
```

Viết kiểm thử đơn vị cho `ProductService`:

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetProductById() {
        Product product = new Product(1L, "Test Product", 100.0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
    }

    @Test
    public void testGetProductById_NotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById(1L);
        });
    }
}
```

#### 3. Sử Dụng Mock Object

Sử dụng `Mockito` để tạo mock object cho `ProductRepository`, giúp kiểm thử các phương thức của `ProductService` mà không cần truy cập vào cơ sở dữ liệu thực tế.

#### 4. Viết Kiểm Thử cho Controller

Để kiểm thử controller, bạn có thể sử dụng `MockMvc` để giả lập các yêu cầu HTTP và kiểm tra phản hồi của controller.

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetProductById() throws Exception {
        Product product = new Product(1L, "Test Product", 100.0);
        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }
}
```

### Best Practices cho Kiểm Thử Đơn Vị

1. **Viết Kiểm Thử Cho Mỗi Đơn Vị**: Mỗi hàm hoặc phương thức nên có kiểm thử đơn vị riêng biệt.
2. **Đặt Tên Rõ Ràng Cho Các Kiểm Thử**: Tên các phương thức kiểm thử nên mô tả rõ ràng về những gì chúng kiểm tra.
3. **Sử Dụng Mock Object**: Sử dụng mock object để kiểm thử các thành phần mà không cần truy cập vào các thành phần khác (ví dụ: cơ sở dữ liệu, dịch vụ bên ngoài).
4. **Kiểm Tra Các Tình Huống Khác Nhau**: Đảm bảo kiểm tra cả các trường hợp thành công và thất bại.
5. **Đảm Bảo Kiểm Thử Nhanh**: Các kiểm thử đơn vị nên thực hiện nhanh chóng để không làm chậm quá trình phát triển.

### Tổng kết

Kiểm thử đơn vị là một phần quan trọng của quá trình phát triển phần mềm, giúp đảm bảo rằng các thành phần của hệ thống hoạt động đúng đắn. Bằng cách sử dụng JUnit, Mockito và các công cụ hỗ trợ khác trong Spring Boot, bạn có thể viết các kiểm thử đơn vị hiệu quả và đáng tin cậy. Việc tuân theo các best practices sẽ giúp bạn tối ưu hóa quá trình kiểm thử và đảm bảo chất lượng mã nguồn.