# Hướng dẫn chạy ứng dụng với Docker Compose

## Tổng quan

Hệ thống này gồm các microservices sau:

1. **API Gateway** (cổng 8090): Điểm đầu vào cho các yêu cầu từ client
2. **Security Service** (cổng 8081): Xử lý xác thực và ủy quyền
3. **User Service** (cổng 8080): Quản lý người dùng
4. **Classroom Service** (cổng 8082): Quản lý lớp học
5. **Email Service** (cổng 8083): Gửi email thông báo

Hệ thống cũng bao gồm các services hỗ trợ:

- **MySQL** (cổng 3307): Cơ sở dữ liệu chính
- **Redis** (cổng 6379): Cache
- **Kafka & ZooKeeper** (cổng 9092, 2181): Xử lý sự kiện
- **Vault** (cổng 8200): Quản lý bí mật
- **Jaeger** (cổng 16686): Distributed tracing
- **Prometheus** (cổng 9090): Theo dõi metrics
- **Grafana** (cổng 3001): Hiển thị dashboard metrics

## Yêu cầu hệ thống

- Docker và Docker Compose
- Ít nhất 8GB RAM
- 20GB dung lượng ổ đĩa trống

## Hướng dẫn cài đặt và chạy

### 1. Chuẩn bị

Đảm bảo đã build tất cả các services trước khi chạy Docker Compose:

```bash
# Build API Gateway
cd api-gateway
./mvnw clean package -DskipTests
cd ..

# Build Security Service
cd security-service/security-service
./mvnw clean package -DskipTests
cd ../..

# Build User Service
cd user-service/user-service
./mvnw clean package -DskipTests
cd ../..

# Build Classroom Service
cd classroom-service/classroomservice
./mvnw clean package -DskipTests
cd ../..

# Build Email Service
cd email-service
./mvnw clean package -DskipTests
cd ..
```

### 2. Chạy toàn bộ hệ thống

Để khởi động toàn bộ hệ thống, chạy lệnh:

```bash
docker-compose up -d
```

Để xem logs của tất cả các services:

```bash
docker-compose logs -f
```

Hoặc logs của một service cụ thể:

```bash
docker-compose logs -f <tên-service>
# Ví dụ: docker-compose logs -f api-gateway
```

### 3. Dừng hệ thống

Để dừng tất cả các services:

```bash
docker-compose down
```

Để dừng và xóa tất cả volumes (mất dữ liệu):

```bash
docker-compose down -v
```

## Truy cập các dịch vụ

- **API Gateway**: http://localhost:8090
- **Security Service**: http://localhost:8081
- **User Service**: http://localhost:8080
- **Classroom Service**: http://localhost:8082
- **Email Service**: http://localhost:8083
- **Vault UI**: http://localhost:8200/ui (Token: hvs.7jrJ9pqL6yy0Nm7gJOZBTwlV)
- **Jaeger UI**: http://localhost:16686
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)

## Vault - Quản lý bí mật

Vault được sử dụng để quản lý các bí mật như mật khẩu, khóa API, và các thông tin nhạy cảm khác. Dịch vụ `vault-init` sẽ tự động cấu hình các bí mật cho từng service khi hệ thống khởi động.

### Thêm bí mật mới

Truy cập Vault UI (http://localhost:8200/ui) sử dụng token `hvs.7jrJ9pqL6yy0Nm7gJOZBTwlV` hoặc sử dụng CLI:

```bash
# Ví dụ thêm một secret mới
docker-compose exec vault vault kv put secret/my-service my-key=my-value
```

### Kiểm tra bí mật

```bash
docker-compose exec vault vault kv get secret/security-service
```

## Sửa lỗi thường gặp

### 1. Vault không khởi động đúng

Kiểm tra logs:
```bash
docker-compose logs vault
```

Đảm bảo rằng script `vault-init.sh` có quyền thực thi:
```bash
chmod +x vault-init.sh
```

### 2. Microservices không thể kết nối đến Vault

Đảm bảo rằng Vault đã được khởi động và các services có cấu hình Vault đúng:
```bash
docker-compose logs vault-init
```

Kiểm tra logs của các microservices:
```bash
docker-compose logs security-service
```

### 3. Lỗi kết nối cơ sở dữ liệu

Đảm bảo MySQL đã khởi động thành công:
```bash
docker-compose logs mysql
```

Kiểm tra các scripts khởi tạo trong thư mục `mysql-init`.

## Theo dõi và giám sát

- Sử dụng Jaeger UI (http://localhost:16686) để theo dõi các trace
- Sử dụng Prometheus (http://localhost:9090) để truy vấn metrics
- Sử dụng Grafana (http://localhost:3001) để xem dashboard theo dõi hệ thống

---

Chúc bạn thành công với hệ thống! 