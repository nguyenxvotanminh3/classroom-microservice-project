package com.kienlongbank.nguyenminh.servicetest;

import com.kienlongbank.nguyenminh.config.TestTracerConfig;
import com.kienlongbank.nguyenminh.user.model.User;
import com.kienlongbank.nguyenminh.user.repository.UserRepository;
import com.kienlongbank.nguyenminh.user.service.impl.UserDubboServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import(TestTracerConfig.class)
public class UserDubboServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDubboServiceImpl userDubboService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("secret");
        user.setActive(true);
    }

    @Test
    void testGetUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Map<String, Object> result = userDubboService.getUserById(1L);

        assertEquals("john_doe", result.get("username"));
        assertTrue((Boolean) result.get("active"));
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> result = userDubboService.getUserById(1L);

        assertEquals("User not found", result.get("error"));
    }

    @Test
    void testGetUsersByIds_Success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(userRepository.findAllById(ids)).thenReturn(List.of(user));

        List<Map<String, Object>> result = userDubboService.getUsersByIds(ids);

        assertEquals(1, result.size());
        assertEquals("john_doe", result.get(0).get("username"));
    }

    @Test
    void testUserExists_True() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertTrue(userDubboService.userExists(1L));
    }

    @Test
    void testUserExists_False() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertFalse(userDubboService.userExists(1L));
    }

    @Test
    void testGetUserByName_Found() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        Map<String, Object> result = userDubboService.getUserByName("john_doe");

        assertEquals("john@example.com", result.get("email"));
    }

    @Test
    void testGetUserByName_NotFound() {
        when(userRepository.findByUsername("not_found")).thenReturn(Optional.empty());

        Map<String, Object> result = userDubboService.getUserByName("not_found");

        assertEquals("User not found", result.get("error"));
    }
}
