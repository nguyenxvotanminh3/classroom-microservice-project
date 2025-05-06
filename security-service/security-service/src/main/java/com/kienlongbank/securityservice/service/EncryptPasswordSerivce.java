package com.kienlongbank.securityservice.service;

import com.kienlongbank.securityservice.config.AppConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptPasswordSerivce {
    private final AppConfiguration appConfiguration;

    public String encryptPassword(String rawPassword) {
        return appConfiguration.passwordEncoder().encode(rawPassword);
    }
}