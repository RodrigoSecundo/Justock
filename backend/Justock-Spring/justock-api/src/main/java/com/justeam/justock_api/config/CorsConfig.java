package com.justeam.justock_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,https://justock.com.br}")
    private String allowedOriginsProperty;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(resolveAllowedOrigins())
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    private String[] resolveAllowedOrigins() {
        List<String> allowedOrigins = new ArrayList<>();
        for (String origin : allowedOriginsProperty.split(",")) {
            String normalized = origin.trim();
            if (!normalized.isEmpty()) {
                allowedOrigins.add(normalized);
            }
        }
        return allowedOrigins.toArray(String[]::new);
    }
}
