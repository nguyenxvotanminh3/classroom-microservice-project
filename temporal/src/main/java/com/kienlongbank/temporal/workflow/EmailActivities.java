package com.kienlongbank.temporal.workflow;

import com.kienlongbank.temporal.dto.EmailRequest;
import com.kienlongbank.temporal.dto.EmailResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface EmailActivities {
    
    /**
     * Activity to send an email notification with classroom data
     * 
     * @param request The email request containing recipient, token, and message
     * @return EmailResponse with the result of the email sending operation
     */
    @ActivityMethod
    EmailResponse sendEmail(EmailRequest request);
} 