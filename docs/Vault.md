### Giới thiệu về Spring Vault Starter

**Spring Vault** là một phần mở rộng của Spring giúp tích hợp ứng dụng với HashiCorp Vault, một công cụ mã nguồn mở để quản lý bí mật, bảo vệ dữ liệu nhạy cảm và cung cấp quyền truy cập an toàn. **Spring Vault Starter** cung cấp một cách dễ dàng để truy cập và quản lý các bí mật từ Vault trong các ứng dụng Spring Boot.

### Ý nghĩa của Spring Vault Starter

1. **Quản lý bí mật an toàn**: Spring Vault giúp bạn quản lý các bí mật như mật khẩu, khóa API, và chứng chỉ một cách an toàn.
2. **Tích hợp liền mạch**: Spring Vault tích hợp dễ dàng với các ứng dụng Spring Boot, giúp bạn truy cập và quản lý các bí mật mà không cần nhiều cấu hình phức tạp.
3. **Bảo mật cao**: Vault cung cấp các cơ chế bảo mật tiên tiến như mã hóa dữ liệu, kiểm soát truy cập chi tiết và theo dõi các hoạt động.
4. **Tiết kiệm thời gian**: Spring Vault giúp tự động hóa việc quản lý bí mật, giảm thiểu thời gian và công sức cần thiết để bảo mật dữ liệu nhạy cảm.

### Cách sử dụng Spring Vault Starter

#### 1. Thêm Dependency vào dự án

Đầu tiên, bạn cần thêm dependency `spring-cloud-starter-vault-config` vào dự án Spring Boot của mình:

```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-vault-config'
}
```

#### 2. Cấu hình Vault

Bạn cần cấu hình Vault trong file `application.yml` để Spring Boot có thể kết nối và truy cập các bí mật từ Vault.

```yaml
spring:
  cloud:
    vault:
      uri: http://localhost:8200         # Địa chỉ của Vault server
      token: s.yourVaultTokenHere        # Token để xác thực với Vault
      kv:
        enabled: true                    # Bật Vault Key-Value backend
        backend: secret                  # Đường dẫn của Key-Value backend
      generic:
        enabled: true                    # Bật truy cập generic secrets
        backend: secret/data             # Đường dẫn của generic secrets backend
```

#### 3. Tạo bí mật trong Vault

Trước khi bạn có thể truy cập bí mật từ ứng dụng Spring Boot, bạn cần tạo các bí mật này trong Vault. Ví dụ, bạn có thể tạo một bí mật chứa thông tin cơ sở dữ liệu:

```sh
vault kv put secret/application-db username=dbuser password=dbpassword
```

#### 4. Truy cập bí mật trong ứng dụng Spring Boot

Spring Vault cho phép bạn truy cập các bí mật từ Vault thông qua các thuộc tính trong `application.yml` hoặc bằng cách sử dụng `@Value` trong mã nguồn.

##### Cấu hình thuộc tính trong `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: ${application-db.username}
    password: ${application-db.password}
```

##### Sử dụng `@Value` trong mã nguồn

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    @Value("${application-db.username}")
    private String username;

    @Value("${application-db.password}")
    private String password;

    // Logic sử dụng username và password
}
```

#### 5. Cấu hình Vault với Role-Based Access Control (RBAC)

Vault hỗ trợ RBAC, cho phép bạn kiểm soát truy cập đến các bí mật dựa trên vai trò. Bạn có thể cấu hình các policy trong Vault để xác định quyền truy cập cho các token khác nhau.

##### Tạo policy trong Vault

```hcl
path "secret/data/application-db" {
  capabilities = ["read"]
}
```

##### Gán policy cho token

```sh
vault token create -policy="read-application-db"
```

### Tổng kết

Spring Vault Starter cung cấp một cách dễ dàng và an toàn để tích hợp ứng dụng Spring Boot với HashiCorp Vault. Bằng cách sử dụng Spring Vault, bạn có thể quản lý và truy cập các bí mật một cách an toàn, bảo vệ dữ liệu nhạy cảm của bạn khỏi các mối đe dọa bảo mật. Việc tích hợp liền mạch và hỗ trợ các tính năng bảo mật tiên tiến giúp Spring Vault trở thành một công cụ hữu ích cho các nhà phát triển khi xây dựng các ứng dụng bảo mật.