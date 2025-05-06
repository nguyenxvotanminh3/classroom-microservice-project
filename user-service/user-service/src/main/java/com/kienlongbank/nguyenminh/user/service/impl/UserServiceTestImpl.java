package com.kienlongbank.nguyenminh.user.service.impl;

import com.kienlongbank.nguyenminh.config.datasource.DataSourceContextHolder;
import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.event.UserRegistrationEvent;
import com.kienlongbank.nguyenminh.user.exception.CreateUserFallbackException;
import com.kienlongbank.nguyenminh.user.exception.UserException;
import com.kienlongbank.nguyenminh.user.mapper.UserMapper;
import com.kienlongbank.nguyenminh.user.model.User;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import com.kienlongbank.nguyenminh.user.service.EncryptPasswordSerivce;
import com.kienlongbank.nguyenminh.user.service.UserService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("userServiceTest")
@Slf4j
@RequiredArgsConstructor
public class UserServiceTestImpl implements UserService {

    private final UserRepository userRepository;
    private final EncryptPasswordSerivce encryptPasswordSerivce;
    private final StreamBridge streamBridge;
    private final UserMapper userMapper;

    private static final String USER_SERVICE = "userService";
    private static final String USER_REGISTRATION_OUTPUT = "user-registration";

    @Override
    @Transactional
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "createUserFallback")
    @RateLimiter(name = USER_SERVICE)
    @Bulkhead(name = USER_SERVICE)
    public UserResponse createUser(UserRequest userRequest) {
        log.info("Checking if username: {} already exists", userRequest.getUsername());
        boolean usernameExists = userRepository.existsByUsername(userRequest.getUsername());
        log.info("Username: {} exists: {}", userRequest.getUsername(), usernameExists);
        
        if (usernameExists) {
            log.warn("Username already exists: {}", userRequest.getUsername());
            throw new UserException("Username already exists");
        }
        
        log.info("Checking if email: {} already exists", userRequest.getEmail());
        boolean emailExists = userRepository.existsByEmail(userRequest.getEmail());
        log.info("Email: {} exists: {}", userRequest.getEmail(), emailExists);
        
        if (emailExists) {
            log.warn("Email already exists: {}", userRequest.getEmail());
            throw new UserException("Email already exists");
        }

        log.info("Creating new user with username: {} and email: {}", userRequest.getUsername(), userRequest.getEmail());
        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setFullName(userRequest.getFullName());
        user.setEmail(userRequest.getEmail());
        user.setPassword(encryptPasswordSerivce.encryptPassword(userRequest.getPassword()));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        log.info("Saving user to database: {}", user.getUsername());
        User savedUser = userRepository.save(user);
        userRepository.flush();
        log.info("User saved successfully with ID: {}", savedUser.getId());
        
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                publishUserRegistrationEvent(savedUser);
            }
        });
        
        return userMapper.convertToUserResponse(savedUser);
    }

    private void publishUserRegistrationEvent(User user) {
        try {
            log.info("Preparing user registration event for user: {}", user.getUsername());
            UserRegistrationEvent event = UserRegistrationEvent.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .registrationTime(LocalDateTime.now())
                    .build();
            
            log.info("Attempting to send event to Kafka topic: {}, event: {}", USER_REGISTRATION_OUTPUT, event);
            boolean sent = streamBridge.send(USER_REGISTRATION_OUTPUT, event);
            if (sent) {
                log.info("User registration event published successfully for user: {}", user.getUsername());
            } else {
                log.error("Failed to publish user registration event for user: {} to topic: {}", 
                    user.getUsername(), USER_REGISTRATION_OUTPUT);
            }
        } catch (Exception e) {
            log.error("Error publishing user registration event for user: {}", user.getUsername(), e);
        }
    }

    public UserResponse createUserFallback(UserRequest userRequest, Throwable t) {
        if (t instanceof UserException) {
            throw (UserException) t;
        }
        String errorMessage = String.format("Fallback for createUser executed for user %s. Error: %s",
                                            userRequest.getUsername(), t.getMessage());
        log.error(errorMessage);
        throw new CreateUserFallbackException(errorMessage, t);
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "getUserByIdFallback")
    @Retry(name = USER_SERVICE)
    @Bulkhead(name = USER_SERVICE)
    public UserResponse getUserById(Long id) {
        String currentDs = DataSourceContextHolder.getDataSourceType();
        log.info("getUserById - Using DataSource: {}", currentDs != null ? currentDs : "default");
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with id: " + id));
        
        log.info("Found user with ID {} in database: {}", id, currentDs);
        return userMapper.convertToUserResponse(user);
    }

    public UserResponse getUserByIdFallback(Long id, Throwable t) {
        log.error("Fallback for getUserById executed. Error: {}", t.getMessage());
        UserResponse fallbackResponse = new UserResponse();
        fallbackResponse.setId(-1L);
        fallbackResponse.setUsername("unavailable");
        fallbackResponse.setFullName("Service Temporarily Unavailable");
        fallbackResponse.setEmail("unavailable@example.com");
        fallbackResponse.setActive(false);
        fallbackResponse.setCreatedAt(LocalDateTime.now());
        fallbackResponse.setUpdatedAt(LocalDateTime.now());
        return fallbackResponse;
    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "getAllUsersFallback")
    @Bulkhead(name = USER_SERVICE)
    public List<UserResponse> getAllUsers() {
        try {
            log.info("Executing getAllUsers with readOnly transaction");
            String currentDs = DataSourceContextHolder.getDataSourceType();
            log.info("Current DataSource: {}", currentDs != null ? currentDs : "default");
            
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("Current transaction readOnly state: {}", isReadOnly);
            
            List<User> users = userRepository.findAll();
            log.info("Found {} users", users.size());
            
            return users.stream()
                    .map(userMapper::convertToUserResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getAllUsers: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<UserResponse> getAllUsersFallback(Throwable t) {
        log.error("Fallback for getAllUsers executed. Error: {}", t.getMessage());
        UserResponse fallbackResponse = new UserResponse();
        fallbackResponse.setId(-1L);
        fallbackResponse.setUsername("unavailable");
        fallbackResponse.setFullName("Service Temporarily Unavailable");
        fallbackResponse.setEmail("unavailable@example.com");
        fallbackResponse.setActive(false);
        fallbackResponse.setCreatedAt(LocalDateTime.now());
        fallbackResponse.setUpdatedAt(LocalDateTime.now());
        return List.of(fallbackResponse);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "updateUserFallback")
    @RateLimiter(name = USER_SERVICE)
    @Bulkhead(name = USER_SERVICE)
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with id: " + id));
        
        if (!existingUser.getUsername().equals(userRequest.getUsername()) &&
                userRepository.existsByUsername(userRequest.getUsername())) {
            throw new UserException("Username already exists");
        }
        
        if (!existingUser.getEmail().equals(userRequest.getEmail()) &&
                userRepository.existsByEmail(userRequest.getEmail())) {
            throw new UserException("Email already exists");
        }
        
        existingUser.setUsername(userRequest.getUsername());
        existingUser.setFullName(userRequest.getFullName());
        existingUser.setEmail(userRequest.getEmail());
        existingUser.setPassword(encryptPasswordSerivce.encryptPassword(userRequest.getPassword()));
        existingUser.setUpdatedAt(LocalDateTime.now());
    
        User updatedUser = userRepository.save(existingUser);
        return userMapper.convertToUserResponse(updatedUser);
    }

    public UserResponse updateUserFallback(Long id, UserRequest userRequest, Throwable t) {
        log.error("Fallback for updateUser executed. Error: {}", t.getMessage());
        UserResponse fallbackResponse = new UserResponse();
        fallbackResponse.setId(id);
        fallbackResponse.setUsername(userRequest.getUsername());
        fallbackResponse.setFullName(userRequest.getFullName());
        fallbackResponse.setEmail(userRequest.getEmail());
        fallbackResponse.setActive(false);
        fallbackResponse.setCreatedAt(LocalDateTime.now());
        fallbackResponse.setUpdatedAt(LocalDateTime.now());
        return fallbackResponse;
    }

    @Override
    @Transactional
    @CircuitBreaker(name = USER_SERVICE)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            log.warn("DELETE USER - User not found with ID: {}", id);
            throw new UserException("User not found with id: " + id);
        }
        
        log.info("DELETE USER - Deleting user with ID {} from database", id);
        userRepository.deleteById(id);
        log.info("DELETE USER - User with ID {} successfully deleted from database", id);
        
        log.info("DELETE USER - User delete operation completed. READ databases will be synchronized within next cycle.");
    }

    @Override
    @Retry(name = "order-api")
    @RateLimiter(name = "order-api")
    public UserResponseLogin findByUsername(String userName, String token) {
        log.info("User Service Request : {}", userName);
        
        Optional<User> optionalUser = userRepository.findByUsername(userName);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException(String.format("Could not find any user with username %s", userName));
        }

        User user = optionalUser.get();
        UserResponseLogin userResponseLogin = new UserResponseLogin();
        BeanUtils.copyProperties(user, userResponseLogin);
        return userResponseLogin;
    }
} 