package com.kienlongbank.emailservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kienlongbank.emailservice.dto.UserRegistrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.cloud.stream.kafka.binder.brokers}")
    private String bootstrapServers;

    private static final String USER_REGISTRATION_TOPIC = "user-registration";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        log.info("Creating KafkaAdmin with bootstrap servers: {}", bootstrapServers);
        var configs = new java.util.HashMap<String, Object>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        log.info("Creating/validating Kafka topic: {}", USER_REGISTRATION_TOPIC);
        return new NewTopic(USER_REGISTRATION_TOPIC, 1, (short) 1);
    }

    @Bean
    public ObjectMapper objectMapper() {
        log.info("Configuring custom ObjectMapper with JavaTimeModule");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * This bean is defined but not actively used as we're leveraging Spring Cloud Stream
     * instead of direct Kafka listeners. It's here to support the KafkaConsumerService if needed.
     */
    @Bean
    public ConsumerFactory<String, UserRegistrationEvent> consumerFactory() {
        log.info("Creating Kafka consumer factory for UserRegistrationEvent (not actively used)");
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "email-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.kienlongbank.emailservice.dto.UserRegistrationEvent");
        
        return new DefaultKafkaConsumerFactory<>(props, 
                new StringDeserializer(),
                new JsonDeserializer<>(UserRegistrationEvent.class, objectMapper(), false));
    }
    
    /**
     * This factory is defined but not actively used as we're leveraging Spring Cloud Stream
     * instead of direct Kafka listeners.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegistrationEvent> kafkaListenerContainerFactory() {
        log.info("Creating Kafka listener container factory (not actively used)");
        ConcurrentKafkaListenerContainerFactory<String, UserRegistrationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
} 