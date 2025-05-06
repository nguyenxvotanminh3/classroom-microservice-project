package com.kienlongbank.emailservice.service;

import com.kienlongbank.emailservice.config.KafkaConfig; // Import nếu cần hằng số topic
import com.kienlongbank.emailservice.dto.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor // Inject EmailService tự động
public class KafkaConsumerService {

    private final EmailService emailService;
    private static final String USER_REGISTRATION_TOPIC = "user-registration"; // Có thể lấy từ KafkaConfig nếu muốn

    @jakarta.annotation.PostConstruct
    public void logInit() {
        log.info("KafkaConsumerService bean initialized but NOT actively listening as we're using Spring Cloud Stream consumer instead.");
    }

} 