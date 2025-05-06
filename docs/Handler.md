### Kiến trúc Controller -> Handler -> Service -> Repository

Trong một ứng dụng Spring Boot, kiến trúc Controller -> Handler -> Service -> Repository là một mẫu thiết kế phổ biến giúp tách biệt các lớp chức năng khác nhau, dễ bảo trì và mở rộng. Dưới đây là mô tả chi tiết về từng lớp trong kiến trúc này:

#### 1. Controller

**Controller** chịu trách nhiệm xử lý các yêu cầu HTTP từ người dùng. Nó đóng vai trò như một giao diện giữa người dùng và hệ thống. Controller không nên chứa bất kỳ logic nghiệp vụ phức tạp nào; thay vào đó, nó sẽ chuyển tiếp các yêu cầu đến lớp Handler hoặc Service.

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductHandler productHandler;

    public ProductController(ProductHandler productHandler) {
        this.productHandler = productHandler;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productHandler.handleGetProductById(id);
        return ResponseEntity.ok(product);
    }
}
```

#### 2. Handler

**Handler** là một lớp bổ sung giúp chia nhỏ logic của Controller. Nó giúp tách biệt các quy trình xử lý yêu cầu cụ thể từ Controller. Handler thường được sử dụng để xử lý các tác vụ như xác thực, kiểm tra quyền truy cập và tiền xử lý dữ liệu trước khi chuyển tiếp yêu cầu đến lớp Service.

Handler cũng có thể được dùng để quản lý các ngoại lệ (exception) cụ thể hoặc định dạng dữ liệu trước khi gửi đến Controller hoặc Service.

```java
@Service
public class ProductHandler {

    private final ProductService productService;

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    public Product handleGetProductById(Long id) {
        // Logic xử lý tiền xử lý, kiểm tra quyền truy cập, v.v.
        return productService.getProductById(id);
    }
}
```

#### 3. Service

**Service** chứa các logic nghiệp vụ chính của ứng dụng. Đây là nơi thực hiện các tác vụ chính như tính toán, áp dụng các quy tắc nghiệp vụ, và quản lý các giao dịch.

Service thường gọi đến các phương thức của lớp Repository để tương tác với cơ sở dữ liệu.

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
    }
}
```

#### 4. Repository

**Repository** chịu trách nhiệm tương tác trực tiếp với cơ sở dữ liệu. Nó cung cấp các phương thức CRUD (Create, Read, Update, Delete) để lưu trữ và truy xuất dữ liệu từ cơ sở dữ liệu.

Trong Spring Data JPA, Repository thường là các interface mở rộng từ `JpaRepository` hoặc `CrudRepository`.

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

### Lưu ý khi sử dụng Handler

- **Xác thực và Kiểm tra Quyền Truy cập**: Handler có thể được sử dụng để xác thực các yêu cầu và kiểm tra quyền truy cập của người dùng trước khi thực hiện các thao tác trong Service.
- **Tiền xử lý Dữ liệu**: Handler có thể thực hiện các bước tiền xử lý như chuẩn hóa hoặc định dạng lại dữ liệu trước khi gửi đến Service.
- **Quản lý Ngoại lệ**: Handler có thể bắt và xử lý các ngoại lệ cụ thể, định dạng lại các lỗi để trả về cho người dùng.
- **Tách biệt Logic**: Sử dụng Handler giúp tách biệt logic xử lý yêu cầu từ Controller, làm cho mã nguồn dễ đọc và dễ bảo trì hơn.

### Tổng kết

Kiến trúc Controller -> Handler -> Service -> Repository giúp tách biệt các lớp chức năng khác nhau trong ứng dụng, làm cho mã nguồn dễ bảo trì và mở rộng. Handler đóng vai trò như một lớp trung gian giúp tách biệt logic xử lý yêu cầu từ Controller, thực hiện các bước tiền xử lý, kiểm tra quyền truy cập và quản lý ngoại lệ trước khi chuyển tiếp yêu cầu đến lớp Service. Việc sử dụng Handler giúp giữ cho Controller và Service gọn gàng và tập trung vào các nhiệm vụ chính của chúng.