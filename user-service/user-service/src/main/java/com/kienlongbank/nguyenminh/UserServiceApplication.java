package com.kienlongbank.nguyenminh;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.kienlongbank.nguyenminh", "com.kienlongbank.userservice.service"})
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Profile("!test")
	@EnableDubbo
	static class DubboConfig {}

}
