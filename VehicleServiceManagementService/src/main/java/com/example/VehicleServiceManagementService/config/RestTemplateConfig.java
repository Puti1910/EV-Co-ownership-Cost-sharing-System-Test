package com.example.VehicleServiceManagementService.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    private final com.example.VehicleServiceManagementService.security.TokenRelayInterceptor tokenRelayInterceptor;

    public RestTemplateConfig(com.example.VehicleServiceManagementService.security.TokenRelayInterceptor tokenRelayInterceptor) {
        this.tokenRelayInterceptor = tokenRelayInterceptor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors(tokenRelayInterceptor)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}

