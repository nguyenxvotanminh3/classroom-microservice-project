package com.kienlongbank.nguyenminh.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationEvent {
    private String username;
    private String email;
    private String fullName;
    private LocalDateTime registrationTime;
} 