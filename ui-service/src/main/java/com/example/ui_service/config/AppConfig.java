package com.example.ui_service.config;

import com.example.ui_service.security.TokenRelayInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class AppConfig {

    private final TokenRelayInterceptor tokenRelayInterceptor;

    public AppConfig(TokenRelayInterceptor tokenRelayInterceptor) {
        this.tokenRelayInterceptor = tokenRelayInterceptor;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(tokenRelayInterceptor));
        return restTemplate;
    }
}