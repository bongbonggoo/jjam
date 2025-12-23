package com.example.festival.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // AI 통신을 위한 RestTemplate 설정
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // JSON 파싱을 위한 ObjectMapper 설정
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}