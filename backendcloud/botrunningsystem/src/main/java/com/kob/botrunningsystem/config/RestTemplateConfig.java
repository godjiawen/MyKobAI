package com.kob.botrunningsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 配置类。
 */
@Configuration
public class RestTemplateConfig {
    /**
     * 查询并返回 getRestTemplate 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRestTemplate with controlled input and output handling.
     *
     * @return 返回 RestTemplate 类型结果；Returns a result of type RestTemplate.
     */
    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
