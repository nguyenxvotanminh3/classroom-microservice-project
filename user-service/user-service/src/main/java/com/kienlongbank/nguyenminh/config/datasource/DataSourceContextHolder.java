package com.kienlongbank.nguyenminh.config.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final List<String> readDataSourceKeys = new ArrayList<>();
    private static final Random random = new Random();

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
        if (readDataSourceKeys.isEmpty()) {
            // Không có READ DataSource nào được cấu hình, trả về WRITE
            return "WRITE";
        }
        // Chọn ngẫu nhiên một READ DataSource
        return readDataSourceKeys.get(random.nextInt(readDataSourceKeys.size()));
    }
} 