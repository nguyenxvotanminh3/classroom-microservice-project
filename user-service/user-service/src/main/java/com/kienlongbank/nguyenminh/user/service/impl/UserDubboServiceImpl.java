package com.kienlongbank.nguyenminh.user.service.impl;

import com.kienlongbank.api.UserService;
import com.kienlongbank.nguyenminh.user.mapper.UserMapper;
import com.kienlongbank.nguyenminh.user.model.User;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

@DubboService(version = "1.0.0", group = "user", timeout = 10000)
@Slf4j
public class UserDubboServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserMapper mapper;
    
    @Autowired
    private MessageSource messageSource;

    @Override
    public Map<String, Object> getUserById(Long userId) {
        log.info("Dubbo: Getting user by ID: {}", userId);
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                Map<String, Object> userMap = mapper.convertUserToMap(user);
                // Thêm thông báo thành công vào kết quả
                userMap.put("message", getMessage("user.get.success"));
                return userMap;
            } else {
                log.warn("Dubbo: User not found with ID: {}", userId);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", getMessage("user.notfound", new Object[]{userId}));
                return errorMap;
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting user by ID: {}", userId, e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", getMessage("user.get.failed.unexpected"));
            return errorMap;
        }
    }

    @Override
    public List<Map<String, Object>> getUsersByIds(List<Long> userIds) {
        log.info("Dubbo: Getting users by IDs: {}", userIds);
        try {
            List<User> users = userRepository.findAllById(userIds);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (User user : users) {
                Map<String, Object> userMap = mapper.convertUserToMap(user);
                // Thêm thông báo thành công vào từng item
                userMap.put("message", getMessage("user.get.success"));
                result.add(userMap);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Dubbo: Error getting users by IDs: {}", userIds, e);
            
            // Trả về danh sách rỗng với thông báo lỗi
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", getMessage("user.list.failed.unexpected"));
            List<Map<String, Object>> errorResult = new ArrayList<>();
            errorResult.add(errorMap);
            return errorResult;
        }
    }

    @Override
    public boolean userExists(Long userId) {
        log.info("Dubbo: Checking if user exists with ID: {}", userId);
        try {
            return userRepository.existsById(userId);
        } catch (Exception e) {
            log.error("Dubbo: Error checking if user exists: {}", userId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserByName(String username) {
        log.info("Dubbo: Getting user by username: {}", username);
        try {
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                Map<String, Object> userMap = mapper.convertUserToMap(user);
                // Thêm thông báo thành công vào kết quả
                userMap.put("message", getMessage("user.getbyname.success"));
                return userMap;
            } else {
                log.warn("Dubbo: User not found with username: {}", username);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", getMessage("user.notfound.username", new Object[]{username}));
                return errorMap;
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting user by username: {}", username, e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", getMessage("user.getbyname.fail"));
            return errorMap;
        }
    }

    @Override
    public String getUserEmailByName(String username) {
        log.info("Dubbo: Getting email for user by username: {}", username);
        try {
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isPresent()) {
                return optionalUser.get().getEmail();
            } else {
                log.warn("Dubbo: User not found with username: {}", username);
                return getMessage("user.notfound.username", new Object[]{username});
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting email for user by username: {}", username, e);
            return getMessage("user.getbyname.fail");
        }
    }
    
    /**
     * Helper method to get localized messages
     */
    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
    
    /**
     * Helper method to get localized messages with arguments
     */
    private String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
} 