package com.kienlongbank.nguyenminh.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.database.sync.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class DatabaseSyncService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSyncService.class);
    
    @Autowired
    private DataSourceConfig dataSourceConfig;
    
    @Autowired
    private Environment env;

    /**
     * Scheduled task to sync data from write database to read databases.
     * Runs at interval specified in application.properties.
     */
    @Scheduled(fixedRateString = "${app.database.sync.interval:30000}")
    public void syncData() {
        log.info("============= DATABASE SYNCHRONIZATION STARTED =============");
        
        try {
            // Get write data source
            DataSource writeDataSource = dataSourceConfig.writeDataSource();
            JdbcTemplate writeJdbcTemplate = new JdbcTemplate(writeDataSource);
            
            // Extract data from write database
            List<Map<String, Object>> users = writeJdbcTemplate.queryForList("SELECT * FROM users");
            log.info("Found {} users in WRITE database", users.size());
            
            // Log some user IDs for tracing
            if (!users.isEmpty()) {
                log.info("User IDs in WRITE database (first 5): {}", 
                        users.stream().limit(5)
                             .map(u -> u.get("id").toString())
                             .reduce((a, b) -> a + ", " + b)
                             .orElse("none"));
            }
            
            // Create connections to read databases
            String[] readUrls = env.getProperty("spring.datasource.read.urls", String[].class);
            if (readUrls == null || readUrls.length == 0) {
                log.warn("No read databases configured, skipping synchronization");
                return;
            }
            
            // Make sure we sync each read database
            for (int i = 0; i < readUrls.length; i++) {
                String readUrl = readUrls[i];
                log.info("Synchronizing to read database {}: {}", i+1, readUrl);
                syncDataToReadDatabase(readUrl, users);
            }
            
            // Verify synchronization status
            verifyDatabaseSynchronization(writeDataSource, readUrls);
            
            log.info("============= DATABASE SYNCHRONIZATION COMPLETED =============");
        } catch (Exception e) {
            log.error("ERROR DURING DATABASE SYNCHRONIZATION: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Verifies that all read databases have the same number of users as the write database
     */
    private void verifyDatabaseSynchronization(DataSource writeDataSource, String[] readUrls) {
        JdbcTemplate writeJdbc = new JdbcTemplate(writeDataSource);
        int writeCount = writeJdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        log.info("WRITE database has {} users after synchronization", writeCount);
        
        // Extract some sample IDs from the write database for validation
        List<Long> writeUserIds = writeJdbc.queryForList("SELECT id FROM users ORDER BY id LIMIT 5", Long.class);
        log.info("Sample user IDs in WRITE database: {}", writeUserIds);
        
        for (int i = 0; i < readUrls.length; i++) {
            HikariDataSource readDataSource = null;
            try {
                readDataSource = new HikariDataSource();
                readDataSource.setJdbcUrl(readUrls[i]);
                readDataSource.setUsername(env.getProperty("spring.datasource.read.username"));
                readDataSource.setPassword(env.getProperty("spring.datasource.read.password"));
                readDataSource.setDriverClassName(env.getProperty("spring.datasource.write.driver-class-name"));
                
                // Không đặt readOnly=true vì service đồng bộ cần quyền sửa READ database
                readDataSource.setReadOnly(false);
                
                JdbcTemplate readJdbc = new JdbcTemplate(readDataSource);
                int readCount = readJdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
                
                // Extract sample IDs from the read database for comparison
                List<Long> readUserIds = readJdbc.queryForList("SELECT id FROM users ORDER BY id LIMIT 5", Long.class);
                
                if (readCount != writeCount) {
                    log.error("SYNCHRONIZATION ISSUE DETECTED: Read database {} has {} users while write database has {}", 
                        i+1, readCount, writeCount);
                    log.error("Read DB{} sample user IDs: {}", i+1, readUserIds);
                    log.error("Write DB sample user IDs: {}", writeUserIds);
                } else {
                    log.info("Read database {} successfully synchronized with {} users", i+1, readCount);
                    log.info("Read DB{} sample user IDs match write DB: {}", i+1, readUserIds);
                }
            } catch (Exception e) {
                log.error("Error verifying synchronization for read database {}: {}", i+1, e.getMessage());
            } finally {
                if (readDataSource != null && !readDataSource.isClosed()) {
                    readDataSource.close();
                }
            }
        }
    }
    
    private void syncDataToReadDatabase(String jdbcUrl, List<Map<String, Object>> users) {
        HikariDataSource readDataSource = null;
        try {
            // Create a datasource for this read database
            readDataSource = new HikariDataSource();
            readDataSource.setJdbcUrl(jdbcUrl);
            readDataSource.setUsername(env.getProperty("spring.datasource.read.username"));
            readDataSource.setPassword(env.getProperty("spring.datasource.read.password"));
            readDataSource.setDriverClassName(env.getProperty("spring.datasource.write.driver-class-name"));
            
            // QUAN TRỌNG: KHÔNG đặt readOnly=true khi cần THAY ĐỔI dữ liệu trong READ database
            // Chỉ có các service bình thường mới cần dùng READ db với readOnly=true
            // Service đồng bộ cần quyền ghi vào READ db
            readDataSource.setReadOnly(false);
            
            JdbcTemplate readJdbcTemplate = new JdbcTemplate(readDataSource);
            
            // Check current data before synchronizing
            int currentCount = readJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            log.info("Read database {} currently has {} users before synchronization", jdbcUrl, currentCount);
            
            // Log some user IDs from the read database before sync
            List<Long> readUserIds = readJdbcTemplate.queryForList(
                "SELECT id FROM users ORDER BY id LIMIT 5", Long.class);
            log.info("User IDs in READ database before sync: {}", readUserIds);
            
            // Clear existing data
            log.info("Clearing existing data in {}", jdbcUrl);
            readJdbcTemplate.execute("DELETE FROM users");
            
            // Insert data
            log.info("Inserting {} users into READ database {}", users.size(), jdbcUrl);
            for (Map<String, Object> user : users) {
                readJdbcTemplate.update(
                    "INSERT INTO users (id, username, full_name, email, password, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    user.get("id"),
                    user.get("username"),
                    user.get("full_name"),
                    user.get("email"),
                    user.get("password"),
                    user.get("active"),
                    user.get("created_at"),
                    user.get("updated_at")
                );
            }
            
            // Verify after synchronization
            int newCount = readJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            
            // Log some user IDs from the read database after sync
            readUserIds = readJdbcTemplate.queryForList(
                "SELECT id FROM users ORDER BY id LIMIT 5", Long.class);
            log.info("User IDs in READ database after sync: {}", readUserIds);
            
            log.info("Successfully synchronized {} users to {}. Database now has {} users", 
                users.size(), jdbcUrl, newCount);
        } catch (Exception e) {
            log.error("Error synchronizing to read database {}: {}", jdbcUrl, e.getMessage(), e);
        } finally {
            // Close the datasource
            if (readDataSource != null && !readDataSource.isClosed()) {
                readDataSource.close();
            }
        }
    }
} 