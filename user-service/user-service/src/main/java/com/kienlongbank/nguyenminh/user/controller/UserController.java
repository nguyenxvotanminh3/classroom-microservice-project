package com.kienlongbank.nguyenminh.user.controller;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.exception.CreateUserFallbackException;
import com.kienlongbank.nguyenminh.user.exception.UserException;
import com.kienlongbank.nguyenminh.user.service.UserService;
import com.kienlongbank.nguyenminh.user.handler.UserHandler;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final MessageSource messageSource;
    private final Tracer tracer;
    private final UserHandler userHandler;

    @Autowired
    public UserController(UserService userService, MessageSource messageSource, Tracer tracer, UserHandler userHandler) {
        this.userService = userService;
        this.messageSource = messageSource;
        this.tracer = tracer;
        this.userHandler = userHandler;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest userRequest, Locale locale) {
        return userHandler.handleCreateUser(userRequest, locale);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, Locale locale) {
        return userHandler.handleGetUserById(id, locale);
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(Locale locale) {
        return userHandler.handleGetAllUsers(locale);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest, Locale locale) {
        return userHandler.handleUpdateUser(id, userRequest, locale);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Locale locale) {
        return userHandler.handleDeleteUser(id, locale);
    }

    @GetMapping("/name/{username}")
    public ResponseEntity<?> findByUsername(@PathVariable String username, Locale locale, HttpServletRequest request) {
        return userHandler.handleFindByUsername(username, locale, request);
    }


}