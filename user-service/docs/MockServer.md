WireMock là một công cụ giả lập (mock) HTTP API mạnh mẽ, giúp bạn tạo ra các dịch vụ web giả lập để kiểm thử các ứng dụng mà không cần kết nối thực tế đến các dịch vụ bên ngoài. WireMock có thể mô phỏng các phản hồi HTTP khác nhau dựa trên các yêu cầu đầu vào, cho phép bạn kiểm thử các kịch bản khác nhau bao gồm cả các trường hợp ngoại lệ.

### 1. Thiết lập WireMock Server

Để sử dụng WireMock trong một dự án Spring Boot, bạn cần thêm các dependencies cần thiết vào file `build.gradle`:

```groovy
dependencies {
    testImplementation 'com.github.tomakehurst:wiremock-jre8:2.33.2'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 2. Cấu hình WireMock trong Spring Boot

Bạn có thể cấu hình WireMock bằng cách tạo một lớp cấu hình để khởi tạo WireMock Server trong các bài kiểm thử.

```java
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = WireMockConfig.class)
public class MyServiceTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(8089); // Default port 8089
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }
    
    // Your test cases here
}
```

### 3. Sử dụng WireMock để mô phỏng các phản hồi HTTP khác nhau

Dưới đây là ví dụ về cách mô phỏng các phản hồi HTTP khác nhau với WireMock trong các bài kiểm thử.

#### a. Mô phỏng phản hồi thành công (HTTP 200)

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Test
public void testSuccessfulResponse() {
    stubFor(get(urlEqualTo("/api/success"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{ \"message\": \"Success\" }")));

    // Call the API and assert the response
}
```

#### b. Mô phỏng lỗi không tìm thấy (HTTP 404)

```java
@Test
public void testNotFoundResponse() {
    stubFor(get(urlEqualTo("/api/notfound"))
            .willReturn(aResponse()
                .withStatus(404)
                .withBody("{ \"error\": \"Not Found\" }")));

    // Call the API and assert the response
}
```

#### c. Mô phỏng lỗi máy chủ (HTTP 500)

```java
@Test
public void testServerErrorResponse() {
    stubFor(get(urlEqualTo("/api/servererror"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{ \"error\": \"Internal Server Error\" }")));

    // Call the API and assert the response
}
```

### 4. Tích hợp WireMock vào bài kiểm thử Spring Boot

Dưới đây là ví dụ hoàn chỉnh về bài kiểm thử sử dụng WireMock để kiểm tra các kịch bản khác nhau.

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
public class MyServiceTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void testSuccessfulResponse() {
        stubFor(get(urlEqualTo("/api/success"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{ \"message\": \"Success\" }")));

        String response = restTemplate.getForObject("http://localhost:8089/api/success", String.class);
        assertEquals("{ \"message\": \"Success\" }", response);
    }

    @Test
    public void testNotFoundResponse() {
        stubFor(get(urlEqualTo("/api/notfound"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withBody("{ \"error\": \"Not Found\" }")));

        try {
            restTemplate.getForObject("http://localhost:8089/api/notfound", String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void testServerErrorResponse() {
        stubFor(get(urlEqualTo("/api/servererror"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{ \"error\": \"Internal Server Error\" }")));

        try {
            restTemplate.getForObject("http://localhost:8089/api/servererror", String.class);
            fail("Should have thrown HttpServerErrorException");
        } catch (HttpServerErrorException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
        }
    }
}
```

### Tổng kết

WireMock là công cụ mạnh mẽ cho việc giả lập các dịch vụ HTTP và rất hữu ích trong việc kiểm thử ứng dụng Spring Boot. Bạn có thể dễ dàng cấu hình và sử dụng WireMock để mô phỏng các phản hồi HTTP khác nhau, bao gồm cả các trường hợp ngoại lệ. Việc sử dụng WireMock giúp bạn đảm bảo rằng ứng dụng của bạn hoạt động đúng đắn ngay cả khi gặp phải các lỗi từ các dịch vụ bên ngoài.