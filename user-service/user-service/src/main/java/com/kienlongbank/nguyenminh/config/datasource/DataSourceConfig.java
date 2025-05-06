package com.kienlongbank.nguyenminh.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@Profile("!test")
@EnableTransactionManagement
public class DataSourceConfig {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSource writeDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(env.getProperty("spring.datasource.write.jdbcUrl"));
        dataSource.setUsername(env.getProperty("spring.datasource.write.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.write.password"));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Cấu hình thêm các thuộc tính quan trọng
        dataSource.setAutoCommit(true); // Để Spring/Hibernate quản lý transaction
        dataSource.setReadOnly(false);  // Datasource này cho phép ghi
        
        // Cấu hình hiệu suất
        dataSource.setMaximumPoolSize(Integer.parseInt(env.getProperty("spring.datasource.write.maximum-pool-size", "10")));
        dataSource.setMinimumIdle(Integer.parseInt(env.getProperty("spring.datasource.write.minimum-idle", "5")));
        dataSource.setIdleTimeout(Long.parseLong(env.getProperty("spring.datasource.write.idle-timeout", "30000")));
        dataSource.setMaxLifetime(Long.parseLong(env.getProperty("spring.datasource.write.max-lifetime", "1800000")));
        
        System.out.println("Configured WRITE DataSource: " + env.getProperty("spring.datasource.write.jdbcUrl") + 
                          " [autoCommit=" + dataSource.isAutoCommit() + 
                          ", readOnly=" + dataSource.isReadOnly() + "]");
        
        return dataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Setting up routing DataSource with WRITE primary and READs secondary");
        DataSourceRouting dataSourceRouting = new DataSourceRouting();

        DataSource writeDataSource = writeDataSource();
        log.info("Created WRITE datasource: {}", writeDataSource);
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("WRITE", writeDataSource);

        String readUrlsProp = env.getProperty("spring.datasource.read.urls");
        if (readUrlsProp != null && !readUrlsProp.isEmpty()) {
            String[] readUrls = readUrlsProp.split(",");
            log.info("Found {} READ DataSources to configure.", readUrls.length);
            for (int i = 0; i < readUrls.length; i++) {
                DataSource readDataSource = createReadDataSource(readUrls[i].trim());
                String key = "READ" + i;
                targetDataSources.put(key, readDataSource);
                DataSourceContextHolder.addReadDataSourceKey(key);
                log.info("Registered READ DataSource: {} -> {}", key, readUrls[i]);
            }
        } else {
            log.warn("WARNING: No READ DataSources configured in application.properties!");
        }

        // Thiết lập WRITE datasource làm mặc định
        dataSourceRouting.setTargetDataSources(targetDataSources);
        dataSourceRouting.setDefaultTargetDataSource(writeDataSource);
        log.info("Set WRITE datasource as the default datasource");
        
        // Đảm bảo gọi afterPropertiesSet để khởi tạo đúng cách
        dataSourceRouting.afterPropertiesSet();
        log.info("DataSource routing setup complete");

        return dataSourceRouting;
    }

    private DataSource createReadDataSource(String url) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(env.getProperty("spring.datasource.read.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.read.password"));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Cấu hình thêm các thuộc tính quan trọng
        dataSource.setAutoCommit(true); // Quan trọng: Bật autoCommit
        dataSource.setReadOnly(true);   // Đánh dấu là read-only connection
        
        // Cấu hình hiệu suất
        dataSource.setMaximumPoolSize(Integer.parseInt(env.getProperty("spring.datasource.read.maximum-pool-size", "10")));
        dataSource.setMinimumIdle(Integer.parseInt(env.getProperty("spring.datasource.read.minimum-idle", "5")));
        dataSource.setIdleTimeout(Long.parseLong(env.getProperty("spring.datasource.read.idle-timeout", "30000")));
        dataSource.setMaxLifetime(Long.parseLong(env.getProperty("spring.datasource.read.max-lifetime", "1800000")));
        
        System.out.println("Configured READ DataSource: " + url + 
                          " [autoCommit=" + dataSource.isAutoCommit() + 
                          ", readOnly=" + dataSource.isReadOnly() + "]");
        
        return dataSource;
    }
    
    /**
     * Configure the transaction manager to use our routing datasource
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        // Make sure to use our routing datasource
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }
} 