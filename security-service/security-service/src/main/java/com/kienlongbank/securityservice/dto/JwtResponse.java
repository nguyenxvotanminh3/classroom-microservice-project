package com.kienlongbank.securityservice.dto;

public class JwtResponse {
    private String token; // JWT
    private String type = "Bearer";
    private String roles;
    private String userName;


    public JwtResponse(String accessToken, String userName, String roles) {
        this.token = accessToken;
        this.userName = userName;
        this.roles = roles;
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return userName;
    }

    public String getRoles() {
        return roles;
    }
}
