package com.kienlongbank.nguyenminh.user.service.impl;

import com.kienlongbank.api.SecurityService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptPasswordSerivce encryptPasswordSerivce;

    @Mock
    private StreamBridge streamBridge;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest userRequest;
    private User user;
    private UserResponse userResponse;
    private final String SECRET = "testSecret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "secret", SECRET);

        // Setup test data
        userRequest = new UserRequest();
        userRequest.setUsername("testuser");
        userRequest.setPassword("password123");
        userRequest.setEmail("test@example.com");
        userRequest.setFullName("Test User");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setFullName("Test User");
        userResponse.setActive(true);
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        try {
            // Start a test transaction
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);

            // Arrange
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(encryptPasswordSerivce.encryptPassword(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.convertToUserResponse(any(User.class))).thenReturn(userResponse);
            when(streamBridge.send(anyString(), any(UserRegistrationEvent.class))).thenReturn(true);

            // Act
            UserResponse result = userService.createUser(userRequest);

            // Simulate transaction commit to trigger synchronizations
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
                sync.beforeCommit(true);
                sync.afterCommit();
            });

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(userRequest.getUsername());
            assertThat(result.getEmail()).isEqualTo(userRequest.getEmail());
            verify(userRepository).save(any(User.class));
            verify(streamBridge).send(eq("user-registration"), any(UserRegistrationEvent.class));

        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUser_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(userRequest))
                .isInstanceOf(UserException.class)
                .hasMessage("Username already exists");
    }

    @Test
    @DisplayName("Should get user by id successfully")
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        UserResponse result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by id")
    void getUserById_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(UserException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    @DisplayName("Should get all users successfully")
    void getAllUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(user);
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.convertToUserResponse(user)).thenReturn(userResponse);

        // Act
        List<UserResponse> results = userService.getAllUsers();

        // Assert
        assertThat(results).isNotNull().hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.convertToUserResponse(user)).thenReturn(userResponse);
        when(encryptPasswordSerivce.encryptPassword(anyString())).thenReturn("newEncodedPassword");

        // Act
        UserResponse result = userService.updateUser(1L, userRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(userRequest.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should find user by username successfully without token validation")
    void findByUsernameWithoutValidation_Success() {
        // Arrange
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserResponseLogin result = userService.findByUsernameWithoutValidation(username);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should throw exception when user not found by username without token validation")
    void findByUsernameWithoutValidation_UserNotFound_ThrowsException() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByUsernameWithoutValidation(username))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Could not find any user with username nonexistent");
    }
} 