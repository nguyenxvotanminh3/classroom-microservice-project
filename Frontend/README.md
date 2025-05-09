# Circuit Breaker Monitor Dashboard

Bảng điều khiển giám sát và kiểm thử mẫu hình Circuit Breaker cho ứng dụng Spring Boot.

## Giới thiệu

Trang web này cung cấp giao diện theo dõi và kiểm thử các chức năng circuit breaker trong hệ thống. Nó cho phép:

1. Theo dõi trạng thái của tất cả các circuit breaker
2. Xem thông tin chi tiết về từng circuit breaker
3. Thực hiện các bài kiểm tra khác nhau để kích hoạt circuit breaker
4. Điều khiển trạng thái circuit breaker (open, closed, half-open)

## Cài đặt và sử dụng

### Yêu cầu

- Ứng dụng Spring Boot đã được khởi chạy với endpoint `/api`
- Các controller circuit breaker đã được triển khai:
  - `CircuitBreakerTestController`
  - `CircuitBreakerLoadController`
  - `CircuitBreakerMonitorController`

### Khởi chạy

1. Chỉ cần mở file `index.html` trong trình duyệt
2. Đảm bảo ứng dụng Spring Boot đang chạy tại `http://localhost:8080/api`

Nếu API endpoint khác, vui lòng điều chỉnh trong file `js/api.js`.

## Tính năng chính

### Giám sát Circuit Breaker

- Hiển thị trạng thái của tất cả circuit breaker
- Hiển thị thông tin chi tiết từng circuit breaker
- Tự động làm mới dữ liệu mỗi 5 giây
- Xem các metrics và cấu hình

### Kiểm thử Stress Test

- **Stress Test**: Gửi nhiều request cùng lúc để kích hoạt circuit breaker
- **Slow Request**: Tạo request có thời gian xử lý chậm để kích hoạt timeout

### Kiểm thử Load Test

- **Connection Pool Test**: Tạo áp lực lên kết nối database 
- **Memory Pressure Test**: Tạo áp lực bộ nhớ để xem tác động lên hệ thống

### Điều khiển Circuit Breaker

- Chuyển đổi trạng thái của circuit breaker (force state)
- Reset các metrics
- Theo dõi quá trình mở/đóng circuit breaker

## Các API được sử dụng

### CircuitBreakerMonitorController

- `GET /circuit-breaker-monitor/names` - Lấy danh sách tên circuit breaker
- `GET /circuit-breaker-monitor/all` - Lấy tất cả thông tin circuit breaker
- `GET /circuit-breaker-monitor/{name}` - Lấy thông tin một circuit breaker
- `GET /circuit-breaker-monitor/{name}/force/{state}` - Chuyển đổi trạng thái
- `GET /circuit-breaker-monitor/{name}/reset` - Reset metrics

### CircuitBreakerTestController

- `POST /circuit-breaker-test/stress-test` - Thực hiện stress test
- `POST /circuit-breaker-test/slow-requests` - Tạo request chậm

### CircuitBreakerLoadController

- `POST /circuit-breaker-load/connection-pool` - Kiểm tra pool kết nối
- `POST /circuit-breaker-load/memory-pressure` - Tạo áp lực bộ nhớ
- `POST /circuit-breaker-load/release-all-memory` - Giải phóng bộ nhớ

## Các ví dụ sử dụng

### Kịch bản 1: Kích hoạt circuit breaker bằng lỗi

1. Mở tab "Stress Test"
2. Đánh dấu "Include Duplicates" để tạo lỗi trùng lặp
3. Nhập số lượng request (30-50)
4. Nhấn "Run Stress Test"
5. Theo dõi panel giám sát để xem khi nào circuit breaker mở

### Kịch bản 2: Kiểm tra timeout

1. Mở tab "Slow Request Test"
2. Đặt thời gian chờ đủ lớn (10-15 giây)
3. Nhấn "Run Slow Request"
4. Theo dõi khi nào request bị timeout

### Kịch bản 3: Kiểm tra chức năng phục hồi

1. Khi circuit breaker đã mở
2. Sử dụng "Force State" để đặt về "HALF-OPEN"
3. Thực hiện một số request thành công
4. Xem circuit breaker có tự động chuyển về CLOSED không

## Tùy chỉnh

Các tham số cần điều chỉnh trong ứng dụng Spring Boot để kiểm thử:

```properties
resilience4j.circuitbreaker.instances.userService.sliding-window-size=20
resilience4j.circuitbreaker.instances.userService.minimum-number-of-calls=10
resilience4j.circuitbreaker.instances.userService.permitted-number-of-calls-in-half-open-state=5
resilience4j.circuitbreaker.instances.userService.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.instances.userService.wait-duration-in-open-state=5s
resilience4j.circuitbreaker.instances.userService.failure-rate-threshold=50
```

## Gỡ lỗi

Nếu gặp vấn đề kết nối API:
1. Kiểm tra console của trình duyệt để xem lỗi
2. Đảm bảo CORS được bật trong ứng dụng Spring Boot
3. Kiểm tra URL API trong file `js/api.js` 