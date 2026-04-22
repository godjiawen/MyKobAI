package com.kob.chatsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 处理 filterChain 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of filterChain with controlled input and output handling.
     *
     * @param http 输入参数；Input parameter.
     * @return 返回 SecurityFilterChain 类型结果；Returns a result of type SecurityFilterChain.
     */
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 鉴权由 WebSocket 路径中的令牌负责，HTTP 层全部放行
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}


