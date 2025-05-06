package com.kienlongbank.classroomservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Class giúp ghi log chi tiết về trạng thái cache của Redis
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisLogInterceptor {

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @PostConstruct
    public void init() {
        log.info("=== Redis Cache Log Interceptor đã được khởi tạo ===");
        logCacheStatus();
    }
    
    /**
     * Log thông tin về các cache đã được khởi tạo
     */
    public void logCacheStatus() {
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            log.info("📌 Redis Cache Manager hiện có {} cache: {}", cacheNames.size(), cacheNames);
            
            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    if (cache instanceof RedisCache) {
                        log.info("   - Cache '{}' (Redis Cache) đã được khởi tạo", cacheName);
                    } else {
                        log.info("   - Cache '{}' ({}) đã được khởi tạo", 
                                cacheName, cache.getClass().getSimpleName());
                    }
                }
            }
            
            if (redisTemplate != null) {
                log.info("📌 Redis Template đã được cấu hình - Key serializer: {}, Value serializer: {}", 
                        redisTemplate.getKeySerializer().getClass().getSimpleName(),
                        redisTemplate.getValueSerializer().getClass().getSimpleName());
            } else {
                log.warn("⚠️ Redis Template chưa được cấu hình");
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi kiểm tra trạng thái cache: {}", e.getMessage());
        }
    }
    
    /**
     * Phương thức này sẽ được gọi định kỳ để kiểm tra trạng thái của Redis cache
     */
    @Bean
    public Runnable cacheStatusChecker() {
        return () -> {
            log.info("=== Kiểm tra trạng thái Redis cache ===");
            logCacheStatus();
        };
    }
} 