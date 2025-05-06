package com.kienlongbank.nguyenminh.user.handler;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.exception.CreateUserFallbackException;
import com.kienlongbank.nguyenminh.user.exception.UserException;
import com.kienlongbank.nguyenminh.user.service.UserService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserHandler {
    private final UserService userService;
    private final MessageSource messageSource;
    private final Tracer tracer;

    public ResponseEntity<?> handleCreateUser(UserRequest userRequest, Locale locale) {
        Span span = tracer.nextSpan().name("createUser").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("Attempting to create user: {}", userRequest.getUsername());
            span.tag("user.username", userRequest.getUsername());

            UserResponse userResponse = userService.createUser(userRequest);

            String msg = messageSource.getMessage("user.create.success", null, locale);
            span.tag("http.status_code", "201");
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", msg, "data", userResponse));

        } catch (UserException e) {
            log.warn("User creation failed for {}: {}", userRequest.getUsername(), e.getMessage());
            String messageKey = e.getMessage().toLowerCase().contains("username") ? "user.create.failed.duplicate.username" : "user.create.failed.duplicate.email";
            String msg = messageSource.getMessage(messageKey, new Object[]{userRequest.getUsername(), userRequest.getEmail()}, locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "409");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));

        } catch (CreateUserFallbackException e) {
            log.error("User creation fallback triggered for {}: {}", userRequest.getUsername(), e.getMessage(), e);
            if (e.getCause() instanceof UserException && e.getCause().getMessage() != null) {
                String errorMessage = e.getCause().getMessage().toLowerCase();
                if (errorMessage.contains("username") || errorMessage.contains("email")) {
                    String messageKey = errorMessage.contains("username") ? 
                            "user.create.failed.duplicate.username" : "user.create.failed.duplicate.email";
                    String msg = messageSource.getMessage(messageKey, 
                            new Object[]{userRequest.getUsername(), userRequest.getEmail()}, locale);
                    span.tag("error", "true");
                    span.tag("error.message", e.getCause().getMessage());
                    span.tag("http.status_code", "409");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
                }
            }
            String msg = messageSource.getMessage("user.create.failed.fallback", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", "FallbackException");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "503");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));

        } catch (Exception e) {
            log.error("Unexpected error during user creation for {}: {}", userRequest.getUsername(), e.getMessage(), e);
            String msg = messageSource.getMessage("user.create.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));

        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleGetUserById(Long id, Locale locale) {
        Span span = tracer.nextSpan().name("getUserById").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.id", String.valueOf(id));
            UserResponse userResponse = userService.getUserById(id);

            if (userResponse.getId() != null && userResponse.getId() == -1L) {
                log.warn("Fallback response received for getUserById: {}", id);
                String msg = messageSource.getMessage("user.get.failed.fallback", null, locale);
                span.tag("error", "true");
                span.tag("error.kind", "FallbackResponse");
                span.tag("http.status_code", "503");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));
            }

            String msg = messageSource.getMessage("user.get.success", null, locale);
            span.tag("http.status_code", "200");
            return ResponseEntity.ok(Map.of("message", msg, "data", userResponse));
        } catch (UserException e) {
            log.warn("Failed to get user with ID {}: {}", id, e.getMessage());
            String msg = messageSource.getMessage("user.notfound", new Object[]{id}, locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("Unexpected error getting user with ID {}: {}", id, e.getMessage(), e);
            String msg = messageSource.getMessage("user.get.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleGetAllUsers(Locale locale) {
        Span span = tracer.nextSpan().name("getAllUsers").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            List<UserResponse> users = userService.getAllUsers();

            if (!users.isEmpty() && users.get(0).getId() != null && users.get(0).getId() == -1L) {
                log.warn("Fallback response received for getAllUsers");
                String msg = messageSource.getMessage("user.list.failed.fallback", null, locale);
                span.tag("error", "true");
                span.tag("error.kind", "FallbackResponse");
                span.tag("http.status_code", "503");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));
            }

            String msg = messageSource.getMessage("user.list.success", null, locale);
            span.tag("user.count", String.valueOf(users.size()));
            span.tag("http.status_code", "200");
            return ResponseEntity.ok(Map.of("message", msg, "data", users));
        } catch (Exception e) {
            log.error("Unexpected error getting all users: {}", e.getMessage(), e);
            String msg = messageSource.getMessage("user.list.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleUpdateUser(Long id, UserRequest userRequest, Locale locale) {
        Span span = tracer.nextSpan().name("updateUser").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.id", String.valueOf(id));
            span.tag("user.username", userRequest.getUsername());

            UserResponse updatedUser = userService.updateUser(id, userRequest);

            if (updatedUser.getId() == null ) {
                log.warn("Potential fallback response received for updateUser: {}", id);
                String msg = messageSource.getMessage("user.update.failed.fallback", null, locale);
                span.tag("error", "true");
                span.tag("error.kind", "FallbackResponse");
                span.tag("http.status_code", "503");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));
            }

            String msg = messageSource.getMessage("user.update.success", null, locale);
            span.tag("http.status_code", "200");
            return ResponseEntity.ok(Map.of("message", msg, "data", updatedUser));
        } catch (UserException e) {
            log.warn("Failed to update user with ID {}: {}", id, e.getMessage());
            String messageKey;
            Object[] messageArgs;
            HttpStatus status;
            if (e.getMessage().toLowerCase().contains("not found")) {
                messageKey = "user.notfound";
                messageArgs = new Object[]{id};
                status = HttpStatus.NOT_FOUND;
            } else {
                messageKey = e.getMessage().toLowerCase().contains("username") ? "user.update.failed.duplicate.username" : "user.update.failed.duplicate.email";
                messageArgs = new Object[]{userRequest.getUsername(), userRequest.getEmail()};
                status = HttpStatus.CONFLICT;
            }
            String msg = messageSource.getMessage(messageKey, messageArgs, locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", String.valueOf(status.value()));
            return ResponseEntity.status(status).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("Unexpected error updating user with ID {}: {}", id, e.getMessage(), e);
            String msg = messageSource.getMessage("user.update.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleDeleteUser(Long id, Locale locale) {
        Span span = tracer.nextSpan().name("deleteUser").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.id", String.valueOf(id));
            userService.deleteUser(id);
            String msg = messageSource.getMessage("user.delete.success", null, locale);
            span.tag("http.status_code", "200");
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (UserException e) {
            log.warn("Failed to delete user with ID {}: {}", id, e.getMessage());
            String msg = messageSource.getMessage("user.notfound", new Object[]{id}, locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("Unexpected error deleting user with ID {}: {}", id, e.getMessage(), e);
            String msg = messageSource.getMessage("user.delete.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }

    public ResponseEntity<?> handleFindByUsername(String username, Locale locale, HttpServletRequest request) {
        Span span = tracer.nextSpan().name("findByUsername").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.username", username);
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                String msg = messageSource.getMessage("user.unauthorized", null, locale);
                span.tag("error", "true");
                span.tag("http.status_code", "401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", msg));
            }
            String token = authHeader.substring(7);
            UserResponseLogin loginInfo = userService.findByUsername(username, token);
            if ("unavailable".equals(loginInfo.getUsername())) {
                log.warn("Fallback response received for findByUsername: {}", username);
                String msg = messageSource.getMessage("user.logininfo.failed.fallback", null, locale);
                span.tag("error", "true");
                span.tag("error.kind", "FallbackResponse");
                span.tag("http.status_code", "503");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));
            }
            span.tag("http.status_code", "200");
            return ResponseEntity.ok(loginInfo);
        } catch (UserException e) {
            log.warn("Failed to find user by username {}: {}", username, e.getMessage());
            String msg = messageSource.getMessage("user.notfound.username", new Object[]{username}, locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
        } catch (RuntimeException e) {
            log.warn("Access denied for user {}: {}", username, e.getMessage());
            String msg = messageSource.getMessage("user.forbidden", new Object[]{username}, "Bạn không có quyền truy cập tài nguyên này", locale);
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "403");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("Unexpected error finding user by username {}: {}", username, e.getMessage(), e);
            String msg = messageSource.getMessage("user.logininfo.failed.unexpected", null, locale);
            span.tag("error", "true");
            span.tag("error.kind", e.getClass().getSimpleName());
            span.tag("error.message", e.getMessage());
            span.tag("http.status_code", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } finally {
            span.end();
        }
    }


} 