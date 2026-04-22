package com.kob.backend.config;

import com.kob.backend.config.filter.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class: defines authentication rules, filter chain and password encoder.
 * Spring Security配置类：定义认证规则、过滤器链和密码编码器。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    /**
     * 处理 passwordEncoder 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of passwordEncoder with controlled input and output handling.
     *
     * @return 返回 PasswordEncoder 类型结果；Returns a result of type PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 处理 authenticationManagerBean 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of authenticationManagerBean with controlled input and output handling.
     *
     * @param authenticationConfiguration 输入参数；Input parameter.
     * @return 返回 AuthenticationManager 类型结果；Returns a result of type AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

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
                        .requestMatchers("/api/user/account/token/", "/api/user/account/register/").permitAll()
                        .requestMatchers("/avatars/**").permitAll()
                        .requestMatchers("/pk/start/game/", "/pk/receive/bot/move/")
                        .access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1')"))
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 处理 webSecurityCustomizer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of webSecurityCustomizer with controlled input and output handling.
     *
     * @return 返回 WebSecurityCustomizer 类型结果；Returns a result of type WebSecurityCustomizer.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/websocket/**");
    }
}