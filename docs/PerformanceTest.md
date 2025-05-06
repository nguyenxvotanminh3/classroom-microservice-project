K6 là một công cụ mã nguồn mở mạnh mẽ để kiểm thử tải (load testing) và kiểm thử hiệu năng (performance testing) của các ứng dụng. K6 được thiết kế để dễ sử dụng, có khả năng mở rộng và cung cấp các tính năng mạnh mẽ để tạo các kịch bản kiểm thử phức tạp. K6 sử dụng JavaScript để định nghĩa các kịch bản kiểm thử, giúp các nhà phát triển dễ dàng tích hợp và viết các bài kiểm thử.

### 1. Thiết lập K6

Để bắt đầu sử dụng K6, bạn cần cài đặt K6 trên máy của mình. Bạn có thể cài đặt K6 bằng cách sử dụng Homebrew trên macOS, Chocolatey trên Windows, hoặc tải về từ trang chủ của K6.

#### a. Cài đặt trên macOS bằng Homebrew

```sh
brew install k6
```

#### b. Cài đặt trên Windows bằng Chocolatey

```sh
choco install k6
```

#### c. Tải về và cài đặt từ trang chủ

Bạn có thể tải về từ [trang chủ K6](https://k6.io/docs/getting-started/installation/) và làm theo hướng dẫn cài đặt.

### 2. Viết kịch bản kiểm thử với K6

Kịch bản kiểm thử trong K6 được viết bằng JavaScript. Dưới đây là một ví dụ kịch bản kiểm thử đơn giản để kiểm thử một ứng dụng Spring Boot.

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 10, // Số lượng người dùng ảo
    duration: '30s', // Thời gian kiểm thử
};

export default function () {
    let res = http.get('http://localhost:8080/api/endpoint');
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time is less than 200ms': (r) => r.timings.duration < 200,
    });
    sleep(1);
}
```

Trong ví dụ này:

- `vus`: Số lượng người dùng ảo (virtual users) thực hiện kiểm thử.
- `duration`: Thời gian kiểm thử.
- `http.get`: Gửi một yêu cầu HTTP GET đến endpoint của ứng dụng Spring Boot.
- `check`: Kiểm tra các điều kiện (status code và thời gian phản hồi).
- `sleep`: Tạm dừng giữa các lần lặp để mô phỏng người dùng thực tế.

### 3. Chạy kịch bản kiểm thử với K6

Để chạy kịch bản kiểm thử, bạn lưu kịch bản vào một file, ví dụ `script.js`, và chạy lệnh sau:

```sh
k6 run script.js
```

K6 sẽ hiển thị kết quả kiểm thử trực tiếp trong terminal, bao gồm các thông số về thời gian phản hồi, tỷ lệ lỗi, và số lượng yêu cầu mỗi giây.

### 4. Phân tích kết quả kiểm thử

Kết quả kiểm thử sẽ bao gồm các thông số như:

- **HTTP response status codes**: Kiểm tra xem tất cả các yêu cầu có phản hồi với mã trạng thái 200 không.
- **Response time**: Thời gian phản hồi của các yêu cầu.
- **Throughput**: Số lượng yêu cầu được xử lý mỗi giây.
- **Error rate**: Tỷ lệ lỗi xảy ra trong quá trình kiểm thử.

Bạn có thể sử dụng các công cụ giám sát và phân tích khác nhau để lưu trữ và phân tích kết quả kiểm thử, chẳng hạn như Grafana và InfluxDB.

### 5. Tích hợp K6 với Grafana và InfluxDB

Để tích hợp K6 với Grafana và InfluxDB, bạn cần cài đặt và cấu hình InfluxDB để lưu trữ kết quả kiểm thử, và sau đó sử dụng Grafana để hiển thị các kết quả này.

#### a. Cấu hình InfluxDB trong kịch bản K6

Thêm cấu hình cho InfluxDB trong kịch bản K6:

```javascript
export let options = {
    vus: 10,
    duration: '30s',
    ext: {
        loadimpact: {
            projectID: 1234567,
            name: "MyTest"
        },
        influxDB: {
            url: 'http://localhost:8086',
            database: 'k6',
            tags: { 
                project: 'my-project'
            },
        },
    },
};
```

#### b. Cấu hình Grafana để đọc dữ liệu từ InfluxDB

Sau khi cài đặt và chạy InfluxDB, bạn cần cấu hình Grafana để đọc dữ liệu từ InfluxDB và tạo các dashboard hiển thị kết quả kiểm thử.

### Tổng kết

K6 là một công cụ mạnh mẽ và linh hoạt cho kiểm thử hiệu năng ứng dụng. Với khả năng sử dụng JavaScript để viết kịch bản kiểm thử và tích hợp dễ dàng với các công cụ giám sát như Grafana và InfluxDB, K6 giúp các nhà phát triển kiểm tra và đảm bảo hiệu suất của ứng dụng Spring Boot một cách hiệu quả.