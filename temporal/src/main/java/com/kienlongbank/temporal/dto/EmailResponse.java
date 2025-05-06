package com.kienlongbank.temporal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailResponse {
    private boolean success;
    private String error;
    private String message;
    private String status;
    
    public boolean isSuccess() {
        if (status != null && status.equalsIgnoreCase("SUCCESS")) {
            return true;
        }
        return success;
    }
} 