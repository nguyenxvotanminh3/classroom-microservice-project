package com.kienlongbank.nguyenminh.config.datasource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(-100) // Ensure this aspect runs before any transaction handling (highest priority)
public class DataSourceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceAspect.class);

    @Pointcut("execution(* com.kienlongbank.nguyenminh.user.service..*.get*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.find*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.select*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.query*(..))")
    public void readOperation() {}

    @Pointcut("execution(* com.kienlongbank.nguyenminh.user.service..*.create*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.save*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.add*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.update*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.edit*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.delete*(..))" +
              "|| execution(* com.kienlongbank.nguyenminh.user.service..*.remove*(..))")
    public void writeOperation() {}

    @Around("readOperation()")
    public Object routeReadOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = getMethodName(joinPoint);
        log.info("ROUTING - Setting READ DataSource for method: {}", methodName);
        
        // Select a random read datasource
        String dataSourceKey = DataSourceContextHolder.getRandomReadDataSourceKey();
        if ("WRITE".equals(dataSourceKey)) {
            log.info("READ DataSources not configured properly, trying with READ1...");
            dataSourceKey = "READ1";
        }
        
        // Set the datasource before method execution
        String originalDataSource = DataSourceContextHolder.getDataSourceType();
        DataSourceContextHolder.setDataSourceType(dataSourceKey);
        log.info("AOP: Switched to READ DataSource: {} for method: {}", dataSourceKey, methodName);
        
        try {
            // Execute the method with the read datasource
            return joinPoint.proceed();
        } finally {
            // Restore original datasource or clear the context
            if (originalDataSource != null) {
                DataSourceContextHolder.setDataSourceType(originalDataSource);
                log.debug("AOP: Restored original DataSource: {}", originalDataSource);
            } else {
                DataSourceContextHolder.clearDataSourceType();
                log.debug("AOP: Cleared DataSource routing");
            }
        }
    }

    @Around("writeOperation()")
    public Object routeWriteOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = getMethodName(joinPoint);
        log.info("ROUTING - Setting WRITE DataSource for method: {}", methodName);
        
        // Set the write datasource before method execution
        String originalDataSource = DataSourceContextHolder.getDataSourceType();
        DataSourceContextHolder.setDataSourceType("WRITE");
        log.info("AOP: Switched to WRITE DataSource for method: {}", methodName);
        
        try {
            // Execute the method with the write datasource
            return joinPoint.proceed();
        } finally {
            // Restore original datasource or clear the context
            if (originalDataSource != null) {
                DataSourceContextHolder.setDataSourceType(originalDataSource);
                log.debug("AOP: Restored original DataSource: {}", originalDataSource);
            } else {
                DataSourceContextHolder.clearDataSourceType();
                log.debug("AOP: Cleared DataSource routing");
            }
        }
    }
    
    private String getMethodName(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
} 