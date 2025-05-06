# Spring Boot Documentation Index

### Giới thiệu về Mẫu Dự Án Spring Boot

#### Tổng quan
Mẫu dự án Spring Boot này được thiết kế để cung cấp một khung sườn hoàn chỉnh, giúp việc triển khai và vận hành ứng dụng trở nên dễ dàng và hiệu quả hơn. Dự án mẫu này tích hợp các công nghệ tiên tiến như tracer và metrics để theo dõi và giám sát hoạt động của hệ thống, đồng thời đảm bảo tính sẵn sàng cao và bảo mật chặt chẽ nhờ sử dụng Keycloak cho việc ủy quyền.

#### Các tính năng chính

1. **Tracer và Metrics:**
    - **Tracer:** Sử dụng OpenTracing hoặc Spring Cloud Sleuth để theo dõi các luồng xử lý yêu cầu, giúp phát hiện và khắc phục sự cố nhanh chóng.
    - **Metrics:** Tích hợp Micrometer và Prometheus để thu thập và phân tích các số liệu quan trọng của hệ thống, cung cấp thông tin chi tiết về hiệu suất và tình trạng hoạt động.

2. **Khả năng chịu lỗi cao:**
    - **Circuit Breaker:** Sử dụng Resilience4j để triển khai các mẫu thiết kế chịu lỗi như Circuit Breaker, giúp hệ thống vẫn hoạt động tốt khi gặp sự cố ở một phần nào đó.
    - **Retry:** Tích hợp cơ chế tự động thử lại khi gặp lỗi tạm thời, đảm bảo tính liên tục của dịch vụ.

3. **Bảo mật cao:**
    - **Keycloak Authorization:** Sử dụng Keycloak để quản lý việc xác thực và ủy quyền, cung cấp các cơ chế bảo mật mạnh mẽ như SSO (Single Sign-On), xác thực đa yếu tố, và quản lý quyền truy cập chi tiết.

#### Lợi ích khi sử dụng mẫu dự án

- **Dễ dàng triển khai và vận hành:** Với các công cụ theo dõi và giám sát, đội ngũ phát triển và vận hành có thể nhanh chóng phát hiện và khắc phục sự cố, tối ưu hóa hiệu suất hệ thống.
- **Tính sẵn sàng cao:** Khả năng chịu lỗi và cơ chế thử lại giúp hệ thống duy trì hoạt động liên tục ngay cả khi có sự cố.
- **Bảo mật mạnh mẽ:** Sử dụng Keycloak giúp quản lý quyền truy cập hiệu quả, bảo vệ dữ liệu và ứng dụng khỏi các mối đe dọa bảo mật.

#### Kết luận
Mẫu dự án Spring Boot này là một lựa chọn lý tưởng cho các doanh nghiệp muốn xây dựng các ứng dụng có khả năng vận hành dễ dàng, sẵn sàng cao và bảo mật tốt. Với các tích hợp công nghệ tiên tiến, dự án sẽ giúp bạn nhanh chóng đưa sản phẩm ra thị trường và duy trì hoạt động ổn định trong môi trường sản xuất.

## Mục lục

1. [Access Log](docs/AccessLog.md)
    - Mô tả cách cấu hình và quản lý Access Log trong Spring Boot.
    
2. [API Documentation](docs/ApiDocs.md)
    - Hướng dẫn tạo và cấu hình tài liệu API sử dụng Swagger và OpenAPI.

3. [Audit](docs/Audit.md)
    - Cách thiết lập và sử dụng chức năng Audit trong ứng dụng Spring Boot.

4. [Caching](docs/Caching.md)
    - Các phương pháp và kỹ thuật caching trong Spring Boot.

5. [Cloud Stream](docs/CloudStream.md)
    - Hướng dẫn sử dụng Spring Cloud Stream cho việc xử lý các luồng dữ liệu.

6. [Clustered Database](docs/ClusterDatabase.md)
    - Cấu hình và quản lý cơ sở dữ liệu dạng cluster trong Spring Boot.

7. [Dubbo](docs/Dubbo.md)
    - Tích hợp Apache Dubbo với Spring Boot.

8. [Handler](docs/Handler.md)
    - Giới thiệu và hướng dẫn sử dụng Handler trong kiến trúc Spring Boot.

9. [Internationalization (I18n)](docs/I18n.md)
    - Cách thức thực hiện quốc tế hóa trong ứng dụng Spring Boot.

10. [Integration Test](docs/IntergrationTest.md)
    - Hướng dẫn và các best practices cho kiểm thử tích hợp trong Spring Boot.

11. [Logging](docs/Logging.md)
    - Cấu hình logging, sử dụng Logback và các kỹ thuật logging nâng cao trong Spring Boot.

12. [Metrics](docs/Metrics.md)
    - Thu thập và giám sát các số liệu với Micrometer và Prometheus.

13. [Mock Server](docs/MockServer.md)
    - Sử dụng WireMock và các công cụ khác để giả lập các dịch vụ bên ngoài.

14. [Performance Test](docs/PerformanceTest.md)
    - Kỹ thuật và công cụ để kiểm thử hiệu năng trong Spring Boot.

15. [REST API Response](docs/RestApiResponse.md)
    - Cách chuẩn hóa và quản lý phản hồi của REST API trong Spring Boot.

16. [REST Client](docs/RestClient.md)
    - Cách cấu hình và sử dụng các REST client như RestTemplate và WebClient.

17. [Security](docs/Security.md)
    - Các kỹ thuật và cấu hình bảo mật cho ứng dụng Spring Boot.

18. [Temporal](docs/Temporal.md)
    - Tích hợp Temporal Workflow với Spring Boot.

19. [Tracing](docs/Tracing.md)
    - Sử dụng Micrometer và OpenTelemetry để theo dõi và giám sát các yêu cầu trong Spring Boot.

20. [Unit Test](docs/UnitTest.md)
    - Hướng dẫn và các best practices cho kiểm thử đơn vị trong Spring Boot.

21. [Validation](docs/Validation.md)
    - Sử dụng Jakarta Bean Validation để xác thực các đối tượng trong Spring Boot.

22. [Vault](docs/Vault.md)
    - Sử dụng Vault quản lý tập trung các dữ liệu bí mật (các key)

23. [Functional Programming](docs/FunctionalProgramming.md)
    - Lập trình hướng hàm, cần áp dụng triệt để.

24. [S3](docs/S3.md)
    - Sử dụng pre-signed trong S3 để upload file trực tiếp lên S3 cũng như hiển thị file từ S3

25. [Resilience4j](docs/Resilience4j.md)
    - Cung cấp các công cụ bảo vệ cho hệ thống, tăng cường khả năng chịu lỗi.