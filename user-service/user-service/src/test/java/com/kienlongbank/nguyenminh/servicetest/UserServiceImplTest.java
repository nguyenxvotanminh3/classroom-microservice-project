package com.kienlongbank.nguyenminh.servicetest;

import com.kienlongbank.nguyenminh.user.dto.UserRequest;
import com.kienlongbank.nguyenminh.user.dto.UserResponse;
import com.kienlongbank.nguyenminh.user.dto.UserResponseLogin;
import com.kienlongbank.nguyenminh.user.exception.UserException;
import com.kienlongbank.nguyenminh.user.model.User;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import com.kienlongbank.nguyenminh.user.service.EncryptPasswordSerivce;
import com.kienlongbank.nguyenminh.user.service.UserService;
import com.kienlongbank.nguyenminh.user.service.impl.UserServiceImpl;
import com.kienlongbank.nguyenminh.user.handler.JwtSecurityHandler;
import com.kienlongbank.nguyenminh.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;

import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptPasswordSerivce encryptPasswordSerivce;

    @Mock
    private StreamBridge streamBridge;

    @Mock
    private JwtSecurityHandler jwtSecurityHandler;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.kienlongbank.api.SecurityService securityService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        // Removed MockitoAnnotations.openMocks(this)
        userRequest = new UserRequest("john_doe", "John Doe", "john@example.com", "password123");
    }

    @Test
    void testCreateUser_Success() {
        User user = new User();
        user.setUsername("john_doe");
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encryptedPassword");

        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("john_doe");
        userResponse.setFullName("John Doe");
        userResponse.setEmail("john@example.com");

        when(userRepository.existsByUsername(userRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(false);
        when(encryptPasswordSerivce.encryptPassword(userRequest.getPassword())).thenReturn("encryptedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.convertToUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse response = userService.createUser(userRequest);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("John Doe", response.getFullName());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void testCreateUser_UsernameAlreadyExists() {
        when(userRepository.existsByUsername(userRequest.getUsername())).thenReturn(true);

        UserException exception = assertThrows(UserException.class, () -> {
            userService.createUser(userRequest);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        when(userRepository.existsByUsername(userRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(true);

        UserException exception = assertThrows(UserException.class, () -> {
            userService.createUser(userRequest);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserById_Success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("john_doe");
        userResponse.setFullName("John Doe");
        userResponse.setEmail("john@example.com");
        userResponse.setActive(true);
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.convertToUserResponse(user)).thenReturn(userResponse);

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertTrue(response.isActive());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserException exception = assertThrows(UserException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
    }

    @Test
    void testUpdateUser_Success() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("john_doe");
        existingUser.setFullName("John Doe");
        existingUser.setEmail("john@example.com");

        UserRequest updatedRequest = new UserRequest("john_doe_updated", "John Doe Updated", "john_updated@example.com", "newpassword123");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("john_doe_updated");
        updatedUser.setFullName("John Doe Updated");
        updatedUser.setEmail("john_updated@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setUsername("john_doe_updated");
        userResponse.setFullName("John Doe Updated");
        userResponse.setEmail("john_updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername(updatedRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(updatedRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.convertToUserResponse(any(User.class))).thenReturn(userResponse);

        UserResponse response = userService.updateUser(1L, updatedRequest);

        assertNotNull(response);
        assertEquals("john_doe_updated", response.getUsername());
    }

    @Test
    void testGetAllUsers_Success() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setFullName("User One");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFullName("User Two");
        user2.setEmail("user2@example.com");

        UserResponse userResponse1 = new UserResponse();
        userResponse1.setId(1L);
        userResponse1.setUsername("user1");
        userResponse1.setFullName("User One");
        userResponse1.setEmail("user1@example.com");

        UserResponse userResponse2 = new UserResponse();
        userResponse2.setId(2L);
        userResponse2.setUsername("user2");
        userResponse2.setFullName("User Two");
        userResponse2.setEmail("user2@example.com");

        List<User> users = List.of(user1, user2);
        List<UserResponse> userResponses = List.of(userResponse1, userResponse2);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.convertToUserResponse(user1)).thenReturn(userResponse1);
        when(userMapper.convertToUserResponse(user2)).thenReturn(userResponse2);

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());
        assertEquals("user1", responses.get(0).getUsername());
    }

    @Test
    void testDeleteUser_Success() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        assertDoesNotThrow(() -> userService.deleteUser(userId));
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        Long userId = 999L;
        when(userRepository.existsById(userId)).thenReturn(false);

        UserException exception = assertThrows(UserException.class, () -> {
            userService.deleteUser(userId);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
    }

    @Test
    void testFindByUsername_InvalidToken() {
        String username = "john_doe";
        String token = "invalid-token";
        when(securityService.validateTokenForUsername(token, username)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.findByUsername(username, token));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }
}
