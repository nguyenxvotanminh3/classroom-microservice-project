package com.kienlongbank.classroomservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ClassroomserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassroomserviceApplication.class, args);
	}

}
