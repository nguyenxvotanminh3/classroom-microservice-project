# Hướng Dẫn Sử Dụng WireMock Trong User Service

## Giới Thiệu

Tài liệu này mô tả cách sử dụng WireMock để kiểm thử User Service, đặc biệt tập trung vào việc xử lý các trường hợp khi username hoặc email đã tồn tại.

## Các Bài Kiểm Thử Đã Triển Khai

### 1. Unit Tests (UserControllerMockTest)

Class `UserControllerMockTest` sử dụng Spring MockMvc và Mockito để kiểm thử chức năng của `UserController` mà không cần khởi động toàn bộ server. Các bài kiểm thử này tập trung vào:

- **createUser_Success**: Kiểm tra luồng tạo user thành công
- **createUser_UsernameExists**: Kiểm tra khi username đã tồn tại
- **createUser_EmailExists**: Kiểm tra khi email đã tồn tại
- **createUser_FallbackWithUsernameError**: Kiểm tra xử lý lỗi khi circuit breaker kích hoạt với lỗi username trùng
- **createUser_FallbackWithEmailError**: Kiểm tra xử lý lỗi khi circuit breaker kích hoạt với lỗi email trùng
- **createUser_GenericFallbackError**: Kiểm tra xử lý lỗi khi circuit breaker kích hoạt với lỗi khác

### 2. Integration Tests (UserServiceWireMockTest)

Class `UserServiceWireMockTest` sử dụng WireMock để giả lập các dịch vụ ngoài như Email Service và Security Service. Các bài kiểm thử này tập trung vào:

- **createUser_Success_SendsEmailNotification**: Kiểm tra luồng tạo user thành công và gửi thông báo qua email
- **createUser_UsernameExists_ReturnsConflict**: Kiểm tra khi username đã tồn tại (yêu cầu cấu hình đặc biệt)
- **createUser_EmailExists_ReturnsConflict**: Kiểm tra khi email đã tồn tại (yêu cầu cấu hình đặc biệt)
- **createUser_EmailServiceFails_StillCreatesUser**: Kiểm tra xử lý lỗi khi Email Service không phản hồi

## Cấu Hình Cần Thiết

### application-test.properties

Tệp `application-test.properties` đã được cấu hình để sử dụng H2 Database trong bộ nhớ, vô hiệu hóa các tính năng cloud và cấu hình Resilience4J. Khi chạy kiểm thử, profile test được kích hoạt bằng annotation `@ActiveProfiles("test")`.

```properties
# Cấu hình kết nối cơ sở dữ liệu H2 cho testing
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# Cấu hình mock service
email.service.url=http://localhost:8089
security.service.url=http://localhost:8089
```

### WireMock Dependency

WireMock đã được thêm vào file pom.xml:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>2.33.2</version>
    <scope>test</scope>
</dependency>
```

## Cách Chạy Các Bài Kiểm Thử

### Chạy Toàn Bộ Bài Kiểm Thử

```bash
mvn test
```

### Chạy Một Nhóm Bài Kiểm Thử Cụ Thể

```bash
mvn test -Dtest=UserControllerMockTest
```

### Chạy Một Bài Kiểm Thử Cụ Thể

```bash
mvn test -Dtest=UserControllerMockTest#createUser_UsernameExists
```

## Mở Rộng Bài Kiểm Thử

### Thêm Bài Kiểm Thử Cho UserControllerMockTest

1. Thêm phương thức mới vào class `UserControllerMockTest`
2. Sử dụng annotation `@Test`
3. Cấu hình behavior sử dụng Mockito
4. Gọi API thông qua MockMvc
5. Kiểm tra kết quả

### Thêm Bài Kiểm Thử Cho UserServiceWireMockTest

1. Thêm phương thức mới vào class `UserServiceWireMockTest`
2. Cấu hình WireMock stubs cho các dịch vụ bên ngoài
3. Gọi API
4. Kiểm tra cả kết quả API và các request được gửi tới các dịch vụ giả lập

## Lưu Ý

- Các bài kiểm thử tích hợp với WireMock có thể cần điều chỉnh nếu thay đổi cấu trúc của API hoặc các dịch vụ bên ngoài
- Khi chạy kiểm thử với Maven, đảm bảo rằng profile test được kích hoạt bằng cách thêm `-Dspring.profiles.active=test`
- Các bài kiểm thử tích hợp yêu cầu port 8089 không được sử dụng bởi ứng dụng khác trong quá trình kiểm thử 