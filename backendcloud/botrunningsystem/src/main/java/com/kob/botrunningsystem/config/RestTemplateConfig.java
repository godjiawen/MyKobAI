package com.kob.botrunningsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 配置类。
 */
@Configuration
public class RestTemplateConfig {
    @Bean
    /**
     * Handles getRestTemplate.
     * ??getRestTemplate?
     */
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}

