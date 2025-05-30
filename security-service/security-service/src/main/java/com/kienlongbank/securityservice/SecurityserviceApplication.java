package com.kienlongbank.securityservice;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class SecurityserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityserviceApplication.class, args);
	}

}
