package com.kienlongbank.classroomservice.client;

import com.kienlongbank.api.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserServiceClient {

    @DubboReference(version = "1.0.0", group = "user", check = false, timeout = 5000)
    private UserService userService;

    /**
     * Get user by ID via the user service
     *
     * @param userId The user ID
     * @return A UserDto object containing user details
     */
    public UserDto getUserById(Long userId) {
        try {
            log.info("Calling user service to get user with ID: {}", userId);
            Map<String, Object> userMap = userService.getUserById(userId);
            
            if (userMap == null || userMap.isEmpty() || userMap.containsKey("error")) {
                log.error("Error or empty response from user service for user ID: {}", userId);
                return null;
            }
            
            return mapToUserDto(userMap);
        } catch (Exception e) {
            log.error("Error calling user service to get user details", e);
            return null;
        }
    }

    /**
     * Get multiple users by IDs via the user service
     *
     * @param userIds List of user IDs
     * @return List of UserDto objects containing user details
     */
    public List<UserDto> getUsersByIds(List<Long> userIds) {
        try {
            log.info("Calling user service to get users with IDs: {}", userIds);
            List<Map<String, Object>> usersList = userService.getUsersByIds(userIds);
            
            if (usersList == null || usersList.isEmpty()) {
                log.error("Error or empty response from user service for user IDs: {}", userIds);
                return new ArrayList<>();
            }
            
            List<UserDto> result = new ArrayList<>();
            for (Map<String, Object> userMap : usersList) {
                UserDto userDto = mapToUserDto(userMap);
                if (userDto != null) {
                    result.add(userDto);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error calling user service to get multiple users", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if a user exists via the user service
     *
     * @param userId The user ID
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(Long userId) {
        try {
            log.info("Calling user service to check if user exists with ID: {}", userId);
            return userService.userExists(userId);
        } catch (Exception e) {
            log.error("Error calling user service to check if user exists", e);
            return false;
        }
    }

    /**
     * Maps a user Map to a UserDto object
     *
     * @param userMap The Map containing user properties
     * @return A UserDto object
     */
    private UserDto mapToUserDto(Map<String, Object> userMap) {
        if (userMap == null) return null;
        
        UserDto userDto = new UserDto();
        
        if (userMap.containsKey("id")) {
            Object idObj = userMap.get("id");
            if (idObj instanceof Long) {
                userDto.setId((Long) idObj);
            } else if (idObj instanceof Integer) {
                userDto.setId(((Integer) idObj).longValue());
            } else if (idObj instanceof String) {
                try {
                    userDto.setId(Long.parseLong((String) idObj));
                } catch (NumberFormatException e) {
                    log.error("Error parsing user ID", e);
                }
            }
        }
        
        if (userMap.containsKey("username")) {
            userDto.setUsername((String) userMap.get("username"));
        }
        
        if (userMap.containsKey("fullName")) {
            userDto.setFullName((String) userMap.get("fullName"));
        }
        
        if (userMap.containsKey("email")) {
            userDto.setEmail((String) userMap.get("email"));
        }
        
        if (userMap.containsKey("active")) {
            Object activeObj = userMap.get("active");
            if (activeObj instanceof Boolean) {
                userDto.setActive((Boolean) activeObj);
            } else if (activeObj instanceof String) {
                userDto.setActive(Boolean.parseBoolean((String) activeObj));
            }
        }
        
        return userDto;
    }

    // Data Transfer Object for User
    public static class UserDto implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private boolean active;
        
        public UserDto() {}
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
    }
} 