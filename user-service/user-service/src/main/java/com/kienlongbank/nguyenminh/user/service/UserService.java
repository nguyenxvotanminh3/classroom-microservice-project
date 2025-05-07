package com.kienlongbank.nguyenminh.user.service;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.model.User;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserResponse createUser(UserRequest userRequest);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UserRequest userRequest);
    void deleteUser(Long id);
    @Retry(name = "order-api")
    @RateLimiter(name = "order-api")
    UserResponseLogin findByUsername(String userName, String token);
}