package com.kienlongbank.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    private String to;
    
    @NotBlank(message = "Token is required")
    @Size(min = 6, message = "Token must be at least 6 characters")
    private String token;
    
    @Size(min = 1, message = "Message cannot be empty")
    private String message;
    
    /**
     * This method returns the message if available, otherwise returns the token.
     * Useful for notification emails where message is the main content.
     */
    public String getMessageContent() {
        return (message != null && !message.isEmpty()) ? message : token;
    }
} 