package com.kienlongbank.temporal.workflow;

import com.kienlongbank.temporal.dto.LoginRequest;
import com.kienlongbank.temporal.dto.LoginResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface LoginActivities {
    
    /**
     * Activity to call the security service API for authentication
     * 
     * @param request The login request
     * @return LoginResponse with authentication result
     */
    @ActivityMethod
    LoginResponse callLoginApi(LoginRequest request);
} 