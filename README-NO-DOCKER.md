# Hướng dẫn Chạy Dự Án không sử dụng Docker

Tài liệu này hướng dẫn cách chạy tất cả các service ở chế độ local không sử dụng Docker Compose.

## Yêu cầu

Trước khi bắt đầu, hãy đảm bảo bạn có:

1. MySQL Server đang chạy trên port 3306
2. Redis Server đang chạy trên port 6379
3. ZooKeeper đang chạy trên port 2181
4. Kafka đang chạy trên port 9092
5. Java 17 trở lên
6. Maven

## Cấu hình Cơ sở dữ liệu

Ứng dụng đã được cấu hình để tự động tạo các cơ sở dữ liệu cần thiết nếu chúng không tồn tại:

- user_write_db
- user_read_db1
- user_read_db2
- classroom_read_db1
- nguyenminh_classroom
- security_db
- email_db

Đảm bảo MySQL đang chạy và thông tin đăng nhập root được cấu hình chính xác trong file application.properties của mỗi service.

## Chạy các dịch vụ hỗ trợ

Bạn có thể sử dụng Docker để chạy các dịch vụ phụ trợ như Zookeeper, Kafka, Redis:

### 1. ZooKeeper và Kafka
```
docker run -d --name zookeeper-local -p 2181:2181 zookeeper:3.8
docker run -d --name kafka-local --link zookeeper-local:zookeeper -p 9092:9092 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper-local:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -e KAFKA_BROKER_ID=1 confluentinc/cp-kafka:7.3.0
```

### 2. MySQL và Redis
```
docker run -d --name mysql-local -p 3306:3306 -e MYSQL_ROOT_PASSWORD=Mink281104@ mysql:8.0
docker run -d --name redis-local -p 6379:6379 redis:latest
```

## Thứ tự khởi động Service

Khởi động các service theo thứ tự sau:

1. Security Service
```
cd security-service/security-service
mvn spring-boot:run
```

2. User Service
```
cd user-service/user-service
mvn spring-boot:run
```

3. Classroom Service
```
cd classroom-service/classroomservice
mvn spring-boot:run
```

4. Email Service
```
cd email-service
mvn spring-boot:run
```

5. API Gateway
```
cd api-gateway
mvn spring-boot:run
```

## Truy cập các Service

Sau khi khởi động tất cả các service, bạn có thể truy cập chúng tại:

- API Gateway: http://localhost:8090
- Security Service: http://localhost:8081/api
- User Service: http://localhost:8080/api
- Classroom Service: http://localhost:8082/api
- Email Service: http://localhost:8083/api

## Kiểm tra API Login

Để kiểm tra API đăng nhập:

```
POST http://localhost:8090/security-api/auth/login
```

Với body request:
```json
{
  "username": "nguyenxvotanminh",
  "password": "yourpassword"
}
```

## Xử lý sự cố

### Vấn đề kết nối cơ sở dữ liệu

Nếu bạn gặp lỗi kết nối cơ sở dữ liệu:

1. Kiểm tra MySQL đang chạy:
```
docker ps | grep mysql-local
```

2. Kiểm tra thông tin kết nối trong file application.properties

3. Kiểm tra kết nối thủ công vào MySQL:
```
mysql -h localhost -P 3306 -u root -p
```

4. Kiểm tra các cơ sở dữ liệu:
```
mysql -u root -p -e "SHOW DATABASES;"
```

### Lỗi kết nối JDBC

Nếu bạn thấy lỗi "Unable to open JDBC Connection":

1. Thông số kết nối URL đã được cấu hình với `createDatabaseIfNotExist=true` để tự động tạo cơ sở dữ liệu
2. Tham số `allowPublicKeyRetrieval=true` đã được thêm để tránh vấn đề xác thực
3. Kiểm tra xem MySQL có chấp nhận kết nối từ localhost không

### Vấn đề với Zookeeper/Kafka

Nếu các dịch vụ không thể kết nối với Zookeeper hoặc Kafka:

1. Kiểm tra container đang chạy:
```
docker ps | grep zookeeper-local
docker ps | grep kafka-local
```

2. Kiểm tra log để xem lỗi cụ thể:
```
docker logs zookeeper-local
docker logs kafka-local
```

### Vấn đề với Redis

Nếu bạn gặp vấn đề với Redis:

1. Kiểm tra Redis đang chạy:
```
docker ps | grep redis-local
```

2. Kiểm tra kết nối đơn giản:
```
redis-cli -h localhost -p 6379 ping
``` 