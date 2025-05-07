package com.kienlongbank.nguyenminh.user.service.impl;

import com.kienlongbank.api.UserService;
import com.kienlongbank.nguyenminh.user.mapper.UserMapper;
import com.kienlongbank.nguyenminh.user.model.User;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@DubboService(version = "1.0.0", group = "user", timeout = 10000)
@Slf4j

public class UserDubboServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper mapper;

    @Override
    public Map<String, Object> getUserById(Long userId) {
        log.info("Dubbo: Getting user by ID: {}", userId);
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                return mapper.convertUserToMap(user);
            } else {
                log.warn("Dubbo: User not found with ID: {}", userId);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "User not found");
                return errorMap;
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting user by ID: {}", userId, e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
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
                result.add(mapper.convertUserToMap(user));
            }
            
            return result;
        } catch (Exception e) {
            log.error("Dubbo: Error getting users by IDs: {}", userIds, e);
            return new ArrayList<>();
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
                return mapper.convertUserToMap(user);
            } else {
                log.warn("Dubbo: User not found with username: {}", username);
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "User not found");
                return errorMap;
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting user by username: {}", username, e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return errorMap;
        }
    }

    @Override
    public String getUserEmailByName(String username) {
        log.info("Dubbo: Getting user by username: {}", username);
        try {
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isPresent()) {
                return optionalUser.get().getEmail();
            } else {
                log.warn("Dubbo: User not found with username: {}", username);
                Map<String, Object> errorMap = new HashMap<>();

                return "Not found";
            }
        } catch (Exception e) {
            log.error("Dubbo: Error getting user by username: {}", username, e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return "Not found";
        }
    }


} 