package com.kienlongbank.classroomservice.service;

import com.kienlongbank.classroomservice.client.UserServiceClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of UserServiceClient để sử dụng trong kiểm thử
 * Thay thế việc gọi thực tế đến UserService thông qua Dubbo
 */
@Component
@Primary
public class UserServiceClientMock extends UserServiceClient {
    
    private Map<Long, UserDto> mockUsers = new HashMap<>();
    
    public UserServiceClientMock() {
        // Khởi tạo một số user mẫu mặc định
        UserDto user1 = new UserDto();
        user1.setId(1L);
        user1.setUsername("teacher1");
        user1.setFullName("Teacher One");
        user1.setEmail("teacher1@example.com");
        user1.setActive(true);
        
        UserDto user2 = new UserDto();
        user2.setId(2L);
        user2.setUsername("teacher2");
        user2.setFullName("Teacher Two");
        user2.setEmail("teacher2@example.com");
        user2.setActive(true);
        
        mockUsers.put(1L, user1);
        mockUsers.put(2L, user2);
    }
    
    /**
     * Thêm user mẫu cho kiểm thử
     */
    public void addMockUser(UserDto user) {
        mockUsers.put(user.getId(), user);
    }
    
    /**
     * Xóa tất cả user mẫu
     */
    public void clearMockUsers() {
        mockUsers.clear();
    }
    
    @Override
    public UserDto getUserById(Long userId) {
        return mockUsers.get(userId);
    }
    
    @Override
    public List<UserDto> getUsersByIds(List<Long> userIds) {
        List<UserDto> result = new ArrayList<>();
        for (Long id : userIds) {
            UserDto user = mockUsers.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }
    
    @Override
    public boolean userExists(Long userId) {
        return mockUsers.containsKey(userId);
    }
} 