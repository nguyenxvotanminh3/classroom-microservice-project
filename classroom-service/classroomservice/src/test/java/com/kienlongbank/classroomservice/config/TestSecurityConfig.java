//package com.kienlongbank.classroomservice.config;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Primary;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.boot.autoconfigure.security.SecurityProperties;
//
//@TestConfiguration
//@EnableWebSecurity
//public class TestSecurityConfig {
//
//    @Bean
//    @Primary
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/**").permitAll()
//            );
//        return http.build();
//    }
//
//    @Bean
//    public SecurityProperties securityProperties() {
//        return new SecurityProperties();
//    }
//}