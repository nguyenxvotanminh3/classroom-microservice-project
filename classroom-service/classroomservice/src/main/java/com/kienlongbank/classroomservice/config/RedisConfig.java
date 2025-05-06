package com.kienlongbank.classroomservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Cấu hình chi tiết cho Redis và theo dõi logs
 * Lưu ý: Cấu hình này chỉ tạo RedisTemplate, CacheManager được định nghĩa trong CacheConfig
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    /**
     * Tạo connection factory cho Redis
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("🔌 Khởi tạo kết nối Redis - host: {}, port: {}", redisHost, redisPort);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
            log.info("🔑 Đã cấu hình password cho Redis");
        }
        
        return new LettuceConnectionFactory(config);
    }
    
    /**
     * Tạo RedisTemplate để thao tác với Redis trực tiếp (không qua Cache)
     * Lưu ý: Bean này KHÔNG ảnh hưởng đến cấu hình CacheManager trong CacheConfig
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("🧰 Khởi tạo Redis Template");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Cấu hình serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());
        
        template.afterPropertiesSet();
        
        log.info("Redis template đã được khởi tạo với key serializer: {} và value serializer: {}", 
                template.getKeySerializer().getClass().getSimpleName(),
                template.getValueSerializer().getClass().getSimpleName());
        
        return template;
    }
    
    /**
     * Bean này được sử dụng để kiểm tra kết nối Redis khi ứng dụng khởi động
     */
    @Bean
    public Runnable redisConnectionCheck(RedisTemplate<String, Object> redisTemplate) {
        return () -> {
            try {
                Boolean pingResult = redisTemplate.getConnectionFactory().getConnection().ping() != null;
                log.info("📌 Kiểm tra kết nối Redis - Ping thành công: {}", pingResult);
            } catch (Exception e) {
                log.error("❌ Không thể kết nối đến Redis: {}", e.getMessage());
            }
        };
    }
} 