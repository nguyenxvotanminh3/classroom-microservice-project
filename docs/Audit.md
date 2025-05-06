Chức năng Audit trong Hibernate giúp theo dõi và ghi lại các thay đổi của dữ liệu trong cơ sở dữ liệu. Điều này rất hữu ích cho việc giám sát, kiểm tra, và khôi phục dữ liệu khi cần thiết. Dưới đây là mô tả chi tiết về chức năng này, ý nghĩa của nó, và cách sử dụng.

### Ý nghĩa của chức năng Audit
1. **Theo dõi thay đổi dữ liệu**: Chức năng Audit giúp ghi lại các thay đổi của các bản ghi trong cơ sở dữ liệu, bao gồm các hoạt động như tạo, cập nhật, và xóa.
2. **Đảm bảo tính minh bạch**: Việc lưu lại lịch sử thay đổi giúp tăng cường tính minh bạch và trách nhiệm trong việc quản lý dữ liệu.
3. **Hỗ trợ khôi phục**: Khi cần khôi phục lại dữ liệu về trạng thái trước đó, chức năng Audit cung cấp các thông tin cần thiết để thực hiện việc này.
4. **Phân tích lịch sử**: Cho phép phân tích các thay đổi của dữ liệu qua thời gian, giúp hiểu rõ hơn về cách dữ liệu đã thay đổi và tại sao.

### Cách sử dụng chức năng Audit trong Hibernate
Hibernate cung cấp một thư viện mở rộng tên là Envers để thực hiện chức năng Audit. Dưới đây là các bước cơ bản để sử dụng Envers:

1. **Thêm phụ thuộc vào dự án**: Thêm thư viện Envers vào dự án của bạn. Nếu sử dụng Gradle, bạn cần thêm phụ thuộc sau vào tệp `build.gradle`:

    ```groovy
    implementation 'org.hibernate:hibernate-envers:5.6.7.Final' // Phiên bản Hibernate hiện tại
    ```

2. **Cấu hình Hibernate Envers**: Cấu hình Hibernate Envers trong tệp `application.properties` hoặc `application.yml`:

    ```properties
    # application.properties
    hibernate.envers.audit_table_suffix = _AUD
    hibernate.envers.revision_field_name = REV
    hibernate.envers.revision_type_field_name = REV_TYPE
    hibernate.envers.audit_strategy = org.hibernate.envers.strategy.ValidityAuditStrategy
    ```

3. **Đánh dấu các thực thể cần audit**: Sử dụng annotation `@Audited` để đánh dấu các thực thể (entity) mà bạn muốn theo dõi thay đổi.

    ```java
    import org.hibernate.envers.Audited;
    import javax.persistence.Entity;
    import javax.persistence.Id;

    @Entity
    @Audited
    public class MyEntity {
        @Id
        private Long id;

        private String name;

        // Các trường khác và các getter, setter
    }
    ```

4. **Sử dụng và kiểm tra dữ liệu audit**: Sử dụng Envers API để truy vấn dữ liệu audit. Dưới đây là ví dụ về cách truy vấn lịch sử thay đổi của một thực thể:

    ```java
    import org.hibernate.envers.AuditReader;
    import org.hibernate.envers.AuditReaderFactory;
    import org.hibernate.envers.query.AuditEntity;
    import javax.persistence.EntityManager;
    import javax.persistence.PersistenceContext;
    import java.util.List;

    public class AuditService {
        @PersistenceContext
        private EntityManager entityManager;

        public List<MyEntity> getRevisions(Long id) {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            return auditReader.createQuery()
                              .forRevisionsOfEntity(MyEntity.class, false, true)
                              .add(AuditEntity.id().eq(id))
                              .getResultList();
        }
    }
    ```

Tính năng audit trong JPA (Java Persistence API) giúp theo dõi và ghi lại các thay đổi của dữ liệu trong cơ sở dữ liệu. Điều này giúp tăng cường tính minh bạch và hỗ trợ cho việc kiểm tra, giám sát, và khôi phục dữ liệu khi cần thiết. Dưới đây là mô tả chi tiết về tính năng này, ý nghĩa của nó, và cách sử dụng.

### Ý nghĩa của tính năng Audit
1. **Theo dõi thay đổi dữ liệu**: Chức năng Audit ghi lại các thay đổi của bản ghi trong cơ sở dữ liệu, bao gồm các hoạt động tạo, cập nhật, và xóa.
2. **Đảm bảo tính minh bạch**: Lịch sử thay đổi dữ liệu giúp tăng cường tính minh bạch và trách nhiệm trong việc quản lý dữ liệu.
3. **Hỗ trợ khôi phục**: Cung cấp thông tin để khôi phục dữ liệu về trạng thái trước đó.
4. **Phân tích lịch sử**: Hỗ trợ phân tích các thay đổi dữ liệu qua thời gian, giúp hiểu rõ hơn về sự thay đổi và nguyên nhân của nó.

### Cách sử dụng tính năng Audit trong JPA
Để thực hiện chức năng audit trong JPA, bạn có thể sử dụng thư viện Spring Data JPA Auditing, hoặc kết hợp Hibernate Envers với JPA. Dưới đây là cách cấu hình và sử dụng tính năng audit với Spring Data JPA Auditing.

#### Sử dụng Spring Data JPA Auditing
1. **Thêm phụ thuộc vào dự án**: Thêm phụ thuộc Spring Data JPA vào dự án của bạn. Nếu bạn sử dụng Gradle, thêm các dòng sau vào tệp `build.gradle`:

    ```groovy
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    ```

2. **Cấu hình Spring Data JPA Auditing**: Kích hoạt chức năng Auditing trong cấu hình Spring Boot của bạn. Thêm annotation `@EnableJpaAuditing` trong lớp cấu hình chính của Spring Boot.

    ```java
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

    @SpringBootApplication
    @EnableJpaAuditing
    public class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }
    ```

3. **Định nghĩa các trường audit trong các thực thể**: Thêm các trường audit như `createdDate`, `lastModifiedDate`, `createdBy`, và `lastModifiedBy` trong các thực thể của bạn. Sử dụng các annotation `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, và `@LastModifiedBy` của Spring Data JPA.

    ```java
    import org.springframework.data.annotation.CreatedDate;
    import org.springframework.data.annotation.LastModifiedDate;
    import org.springframework.data.jpa.domain.support.AuditingEntityListener;

    import javax.persistence.*;
    import java.time.LocalDateTime;

    @Entity
    @EntityListeners(AuditingEntityListener.class)
    public class MyEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        @CreatedDate
        @Column(updatable = false)
        private LocalDateTime createdDate;

        @LastModifiedDate
        private LocalDateTime lastModifiedDate;

        // Getters and setters
    }
    ```

4. **Cấu hình AuditorAware**: Để tự động điền các trường `createdBy` và `lastModifiedBy`, bạn cần định nghĩa một bean `AuditorAware` để cung cấp thông tin về người dùng hiện tại.

    ```java
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.domain.AuditorAware;
    import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

    import java.util.Optional;

    @Configuration
    @EnableJpaAuditing(auditorAwareRef = "auditorProvider")
    public class AuditingConfig {

        @Bean
        public AuditorAware<String> auditorProvider() {
            return new AuditorAwareImpl();
        }
    }

    public class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // Trả về người dùng hiện tại (ví dụ từ SecurityContext)
            return Optional.of("admin"); // Cần thay thế bằng logic thực tế
        }
    }
    ```

### Kết hợp Hibernate Envers với JPA
Nếu bạn muốn sử dụng Hibernate Envers để audit chi tiết hơn, bạn có thể kết hợp nó với JPA như sau:

1. **Thêm phụ thuộc Hibernate Envers**: Thêm phụ thuộc Hibernate Envers vào dự án của bạn:

    ```groovy
    implementation 'org.hibernate:hibernate-envers:5.6.7.Final' // Phiên bản Hibernate hiện tại
    ```

2. **Cấu hình Hibernate Envers**: Thực hiện cấu hình tương tự như phần Hibernate đã mô tả ở trên, bao gồm việc thêm các annotation `@Audited` vào các thực thể và cấu hình trong tệp `application.properties`.

