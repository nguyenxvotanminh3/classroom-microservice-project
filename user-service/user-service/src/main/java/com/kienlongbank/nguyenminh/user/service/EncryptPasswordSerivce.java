package com.kienlongbank.nguyenminh.user.service;

import com.kienlongbank.nguyenminh.config.AppConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EncryptPasswordSerivce {
    private final AppConfiguration appConfiguration;


    public String encryptPassword(String rawPassword) {
        return appConfiguration.passwordEncoder().encode(rawPassword);
    }
}
