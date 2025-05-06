package com.kienlongbank.emailservice.service;

import com.kienlongbank.emailservice.dto.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationConsumer {

    private final EmailService emailService;
    
    @EventListener(ApplicationStartedEvent.class)
    public void logKafkaConfiguration() {
        log.info("UserRegistrationConsumer initialized and ready to process messages");
        log.info("Consumer is listening to 'user-registration' topic with 'email-service-group' group");
    }

    /**
     * Spring Cloud Stream consumer function to process user registration events
     * This bean will be automatically picked up by Spring Cloud Stream and bound to the
     * handleUserRegistration-in-0 binding defined in application.properties
     */
    @Bean
    public Consumer<Message<UserRegistrationEvent>> handleUserRegistration() {
        log.info("Creating handleUserRegistration bean function for Spring Cloud Stream");
        return message -> {
            String messageId = message.getHeaders().containsKey("id") ? message.getHeaders().get("id").toString() : "unknown";
            log.info("Processing message ID: {}", messageId);
            
            try {
                log.info("=== RECEIVED MESSAGE FROM KAFKA ===");
                log.info("Message headers: {}", message.getHeaders());
                log.info("Message payload type: {}", message.getPayload() != null ? message.getPayload().getClass().getName() : "null");
                log.info("Message payload: {}", message.getPayload());
                
                UserRegistrationEvent event = message.getPayload();
                
                if (event == null) {
                    log.warn("Received null user registration event");
                    return;
                }
                
                // Check for duplicate processing by logging unique identifiers
                log.info("Processing user registration event for user: {}, email: {}, fullName: {}, time: {}", 
                    event.getUsername(), event.getEmail(), event.getFullName(), event.getRegistrationTime());
                
                // Send welcome email to the newly registered user
                String welcomeMessage = String.format(
                    "Kính chào %s,\n\n" +
                    "Chúc mừng bạn đã đăng ký tài khoản thành công tại hệ thống của chúng tôi! \n\n" +
                    "Thông tin tài khoản của bạn:\n" +
                    "- Tên đăng nhập: %s\n" +
                    "- Email: %s\n\n" +
                    "Tài khoản của bạn đã sẵn sàng để sử dụng. Vui lòng đăng nhập để trải nghiệm các dịch vụ.\n\n" +
                    "Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ Kienlongbank",
                    event.getFullName() != null ? event.getFullName() : event.getUsername(),
                    event.getUsername(),
                    event.getEmail()
                );
                
                log.info("Sending welcome email to user: {} at email: {}", event.getUsername(), event.getEmail());
                emailService.sendNotificationEmail(event.getEmail(), welcomeMessage);
                log.info("Welcome email sent successfully to: {}", event.getEmail());
                
            } catch (Exception e) {
                log.error("Error processing user registration event for message ID: {}", messageId, e);
                // Even with errors, we acknowledge to avoid reprocessing the same message
            }
        };
    }

    /**
     * Process a user registration event
     * This method is called from the test controller
     */
    public void processUserRegistration(UserRegistrationEvent event) {
        try {
            log.info("Processing user registration event manually");
            
            if (event == null) {
                log.warn("Received null user registration event");
                return;
            }
            
            log.info("Processing user registration event for user: {}, email: {}, fullName: {}, time: {}", 
                event.getUsername(), event.getEmail(), event.getFullName(), event.getRegistrationTime());
            
            try {
                // Send welcome email to the newly registered user
                String welcomeMessage = String.format(
                    "Kính chào %s,\n\n" +
                    "Chúc mừng bạn đã đăng ký tài khoản thành công tại hệ thống của chúng tôi! \n\n" +
                    "Thông tin tài khoản của bạn:\n" +
                    "- Tên đăng nhập: %s\n" +
                    "- Email: %s\n\n" +
                    "Tài khoản của bạn đã sẵn sàng để sử dụng. Vui lòng đăng nhập để trải nghiệm các dịch vụ.\n\n" +
                    "Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ Kienlongbank",
                    event.getFullName() != null ? event.getFullName() : event.getUsername(),
                    event.getUsername(),
                    event.getEmail()
                );
                
                log.info("Sending welcome email to user: {} at email: {}", event.getUsername(), event.getEmail());
                emailService.sendNotificationEmail(event.getEmail(), welcomeMessage);
                log.info("Welcome email sent successfully to: {}", event.getEmail());
            } catch (Exception e) {
                log.error("Failed to send welcome email", e);
            }
        } catch (Exception e) {
            log.error("Error processing user registration event", e);
        }
    }
} 