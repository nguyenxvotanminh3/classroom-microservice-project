package com.kienlongbank.nguyenminh.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseLogin {
    private Long id;
    private String username;
    private String password;
    private boolean active;
}
