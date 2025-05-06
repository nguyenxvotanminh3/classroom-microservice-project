package com.kienlongbank.emailservice.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final HttpServletRequest request;
    @Value("${email.sender}")
    private String email;
    private static final String EMAIL_SERVICE = "emailService";
    
    /**
     * Send password reset email with token
     * 
     * @param to Recipient email address
     * @param token Reset password token
     */
    @CircuitBreaker(name = EMAIL_SERVICE, fallbackMethod = "sendEmailFallback")
    @Retry(name = EMAIL_SERVICE)
    @Bulkhead(name = EMAIL_SERVICE)
    @RateLimiter(name = EMAIL_SERVICE)
    public void sendEmail(String to, String token) {

        log.info("Sending email to: {}", to);
        
        try {
            String baseUrl = request.getRequestURL().toString()
                    .replace(request.getRequestURI(), "");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("worknguyenvotanminh@gmail.com");
            message.setTo(to);
            message.setSubject("Reset Your Password");
            message.setText("This is your token: " + token + "\n" +
                    "Click the link below to change your password:\n" +
                    "https://thezwallet.netlify.app/Zwallet/pages/changethepass");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to: {}", to, e);
            throw e;
        }
    }
    
    /**
     * Fallback method when email sending fails
     */
    public void sendEmailFallback(String to, String token, Throwable t) {
        log.error("Email sending failed. Fallback executed for recipient: {}, Error: {}", to, t.getMessage());
        // In a real-world app, you might:
        // 1. Store failed emails in a database for retry later
        // 2. Send a notification to an admin
        // 3. Log to a monitoring system
        
        // For now, we'll just log the failure
        log.warn("FALLBACK: Email service is currently unavailable. The email to {} will be sent later.", to);
    }
    
    /**
     * Send verification email
     * 
     * @param to Recipient email address
     * @param verificationCode Verification code
     */
    @CircuitBreaker(name = EMAIL_SERVICE, fallbackMethod = "sendVerificationEmailFallback")
    @Retry(name = EMAIL_SERVICE)
    @Bulkhead(name = EMAIL_SERVICE)
    public void sendVerificationEmail(String to, String verificationCode) {
        log.info("Sending verification email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email);
            message.setTo(to);
            message.setSubject("Verify Your Email Address");
            message.setText("Your verification code is: " + verificationCode + "\n" +
                    "Please use this code to verify your account.");

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw e;
        }
    }

    /**
     * Fallback method when verification email sending fails
     */
    public void sendVerificationEmailFallback(String to, String verificationCode, Throwable t) {
        log.error("Verification email sending failed. Fallback executed for recipient: {}, Error: {}", 
                to, t.getMessage());
        log.warn("FALLBACK: Email service is currently unavailable. The verification email to {} will be sent later.", to);
    }
    
    /**
     * Send notification email
     * 
     * @param to Recipient email address
     * @param message Notification message
     */
    @CircuitBreaker(name = EMAIL_SERVICE, fallbackMethod = "sendNotificationEmailFallback")
    @Retry(name = EMAIL_SERVICE)
    @Bulkhead(name = EMAIL_SERVICE)
    @RateLimiter(name = EMAIL_SERVICE)
    public void sendNotificationEmail(String to, String message) {
        log.info("Sending notification email to: {}", to);
        
        try {
            SimpleMailMessage emailMessage = new SimpleMailMessage();
            emailMessage.setFrom("worknguyenvotanminh@gmail.com");
            emailMessage.setTo(to);
            emailMessage.setSubject("Important Notification");
            emailMessage.setText("Notification: " + message);

            mailSender.send(emailMessage);
            log.info("Notification email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send notification email to: {}", to, e);
            throw e;
        }
    }
    
    /**
     * Fallback method when notification email sending fails
     */
    public void sendNotificationEmailFallback(String to, String message, Throwable t) {
        log.error("Notification email sending failed. Fallback executed for recipient: {}, Error: {}", 
                to, t.getMessage());
        log.warn("FALLBACK: Email service is currently unavailable. The notification email to {} will be sent later.", to);
    }
} 