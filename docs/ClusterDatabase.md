### AbstractRoutingDataSource trong Spring

`AbstractRoutingDataSource` là một lớp trừu tượng trong Spring Framework cho phép định tuyến động các kết nối đến các nguồn dữ liệu khác nhau tại runtime. Điều này rất hữu ích khi bạn cần triển khai cơ chế đọc/ghi tách biệt, ví dụ như một cơ sở dữ liệu cluster với một instance dành cho ghi (write) và nhiều instance dành cho đọc (read).

### Ý nghĩa của AbstractRoutingDataSource
1. **Tăng cường hiệu suất**: Cho phép cân bằng tải các yêu cầu đọc đến nhiều instance cơ sở dữ liệu, giúp giảm tải cho một instance duy nhất.
2. **Đảm bảo tính sẵn sàng**: Nếu một instance đọc gặp sự cố, các yêu cầu có thể được định tuyến đến các instance khác.
3. **Tách biệt đọc/ghi**: Giúp tối ưu hóa các hoạt động cơ sở dữ liệu bằng cách tách biệt các yêu cầu ghi và đọc.

### Cách triển khai AbstractRoutingDataSource trong Spring Boot 3.x

#### 1. Thêm các phụ thuộc cần thiết vào tệp `build.gradle`
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-jdbc'
implementation 'com.zaxxer:HikariCP'
```

#### 2. Cấu hình các nguồn dữ liệu trong `application.yml`
```yaml
spring:
  datasource:
    write:
      url: jdbc:mysql://localhost:3306/write_db
      username: write_user
      password: write_pass
    read:
      urls:
        - jdbc:mysql://localhost:3307/read_db_1
        - jdbc:mysql://localhost:3308/read_db_2
      username: read_user
      password: read_pass
  jpa:
    properties:
      hibernate:
        hbm2ddl:
          auto: update
```

#### 3. Định nghĩa DataSourceRouting

Tạo lớp `DataSourceRouting` kế thừa từ `AbstractRoutingDataSource` để định tuyến các yêu cầu đến các nguồn dữ liệu phù hợp.

```java
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DataSourceRouting extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
```

Tạo lớp `DataSourceContextHolder` để giữ thông tin về loại DataSource hiện tại (read hoặc write).

```java
public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setDataSourceType(String dataSourceType) {
        CONTEXT_HOLDER.set(dataSourceType);
    }

    public static String getDataSourceType() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSourceType() {
        CONTEXT_HOLDER.remove();
    }
}
```

#### 4. Cấu hình các nguồn dữ liệu và AbstractRoutingDataSource

Tạo cấu hình Spring để thiết lập các DataSource và AbstractRoutingDataSource.

```java
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    public DataSource readDataSource() {
        DataSourceRouting dataSourceRouting = new DataSourceRouting();

        DataSource writeDataSource = writeDataSource();
        DataSource readDataSource1 = createReadDataSource(env.getProperty("spring.datasource.read.urls[0]"));
        DataSource readDataSource2 = createReadDataSource(env.getProperty("spring.datasource.read.urls[1]"));

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("WRITE", writeDataSource);
        targetDataSources.put("READ1", readDataSource1);
        targetDataSources.put("READ2", readDataSource2);

        dataSourceRouting.setTargetDataSources(targetDataSources);
        dataSourceRouting.setDefaultTargetDataSource(writeDataSource);

        return dataSourceRouting;
    }

    private DataSource createReadDataSource(String url) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(env.getProperty("spring.datasource.read.username"))
                .password(env.getProperty("spring.datasource.read.password"))
                .build();
    }
}
```

#### 5. Sử dụng AOP để chuyển đổi giữa các nguồn dữ liệu
Tạo aspect để chuyển đổi giữa các nguồn dữ liệu đọc và ghi.

```java
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataSourceAspect {

    @Before("execution(* com.example.service.*.get*(..))")
    public void setReadDataSourceType() {
        DataSourceContextHolder.setDataSourceType("READ" + (Math.random() < 0.5 ? "1" : "2"));
    }

    @Before("execution(* com.example.service.*.save*(..)) || execution(* com.example.service.*.update*(..))")
    public void setWriteDataSourceType() {
        DataSourceContextHolder.setDataSourceType("WRITE");
    }
}
```

Để xử lý động các read instance, bạn có thể tạo một danh sách các instance đọc từ cấu hình và sử dụng cơ chế cân bằng tải động để chọn một instance ngẫu nhiên cho mỗi yêu cầu đọc. Dưới đây là cách bạn có thể triển khai:

### 1. Cấu hình các nguồn dữ liệu trong `application.yml`
```yaml
spring:
  datasource:
    write:
      url: jdbc:mysql://localhost:3306/write_db
      username: write_user
      password: write_pass
    read:
      urls:
        - jdbc:mysql://localhost:3307/read_db_1
        - jdbc:mysql://localhost:3308/read_db_2
        # Thêm các URL read instance khác tại đây
      username: read_user
      password: read_pass
  jpa:
    properties:
      hibernate:
        hbm2ddl:
          auto: update
```

### 2. Định nghĩa DataSourceRouting
Tạo lớp `DataSourceRouting` kế thừa từ `AbstractRoutingDataSource` để định tuyến các yêu cầu đến các nguồn dữ liệu phù hợp.

```java
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DataSourceRouting extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
```

Tạo lớp `DataSourceContextHolder` để giữ thông tin về loại DataSource hiện tại (read hoặc write).

```java
public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final List<String> readDataSourceKeys = new ArrayList<>();

    public static void setDataSourceType(String dataSourceType) {
        CONTEXT_HOLDER.set(dataSourceType);
    }

    public static String getDataSourceType() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSourceType() {
        CONTEXT_HOLDER.remove();
    }

    public static void addReadDataSourceKey(String key) {
        readDataSourceKeys.add(key);
    }

    public static String getRandomReadDataSourceKey() {
        return readDataSourceKeys.get(new Random().nextInt(readDataSourceKeys.size()));
    }
}
```

### 3. Cấu hình các nguồn dữ liệu và AbstractRoutingDataSource
Tạo cấu hình Spring để thiết lập các DataSource và AbstractRoutingDataSource.

```java
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    public DataSource readDataSource() {
        DataSourceRouting dataSourceRouting = new DataSourceRouting();

        DataSource writeDataSource = writeDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("WRITE", writeDataSource);

        String[] readUrls = env.getProperty("spring.datasource.read.urls", String[].class);
        for (int i = 0; i < readUrls.length; i++) {
            DataSource readDataSource = createReadDataSource(readUrls[i]);
            String key = "READ" + i;
            targetDataSources.put(key, readDataSource);
            DataSourceContextHolder.addReadDataSourceKey(key);
        }

        dataSourceRouting.setTargetDataSources(targetDataSources);
        dataSourceRouting.setDefaultTargetDataSource(writeDataSource);

        return dataSourceRouting;
    }

    private DataSource createReadDataSource(String url) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(env.getProperty("spring.datasource.read.username"))
                .password(env.getProperty("spring.datasource.read.password"))
                .build();
    }
}
```

### 4. Sử dụng AOP để chuyển đổi giữa các nguồn dữ liệu
Tạo aspect để chuyển đổi giữa các nguồn dữ liệu đọc và ghi.

```java
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataSourceAspect {

    @Before("execution(* com.example.service.*.get*(..))")
    public void setReadDataSourceType() {
        DataSourceContextHolder.setDataSourceType(DataSourceContextHolder.getRandomReadDataSourceKey());
    }

    @Before("execution(* com.example.service.*.save*(..)) || execution(* com.example.service.*.update*(..))")
    public void setWriteDataSourceType() {
        DataSourceContextHolder.setDataSourceType("WRITE");
    }
}
```

