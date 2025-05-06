package com.kienlongbank.emailservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class EmailServiceApplication {

	public static void main(String[] args) {
		// Configure lower log level programmatically
		System.setProperty("logging.level.org.apache.kafka", "WARN");
		System.setProperty("logging.level.org.springframework.kafka", "INFO");
		SpringApplication.run(EmailServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner logFunctionBeans(ApplicationContext ctx) {
		return args -> {
			log.info("Email Service started successfully");
			log.info("Listening for user registration events on the 'user-registration' Kafka topic");
			log.info("Email service is ready to send welcome emails");
			
			log.info("Checking registered function beans:");
			String[] beanNames = ctx.getBeanNamesForType(java.util.function.Consumer.class);
			for (String beanName : beanNames) {
				log.info("Found consumer function bean: {}", beanName);
			}
			
			String[] beanNamesForType = ctx.getBeanNamesForType(org.springframework.cloud.stream.function.StreamBridge.class);
			if (beanNamesForType.length > 0) {
				log.info("StreamBridge bean is available for messaging");
			} else {
				log.warn("StreamBridge bean is NOT available");
			}
		};
	}
}
