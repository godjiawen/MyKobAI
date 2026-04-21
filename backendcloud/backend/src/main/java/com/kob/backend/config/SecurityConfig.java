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
     * Exposes the BCrypt password encoder as a Spring bean.
     * 将BCrypt密码编码器暴露为Spring Bean。
     */
    @Bean
    /**
     * Handles passwordEncoder.
     * ??passwordEncoder?
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a Spring bean for use in login services.
     * 将AuthenticationManager暴露为Spring Bean供登录服务使用。
     */
    @Bean
    /**
     * Handles authenticationManagerBean.
     * ??authenticationManagerBean?
     */
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures the security filter chain: disables CSRF, sets stateless sessions and defines authorization rules.
     * 配置安全过滤器链：禁用CSRF、设置无状态会话并定义授权规则。
     */
    @Bean
    /**
     * Handles filterChain.
     * ??filterChain?
     */
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
     * Ignores WebSocket endpoint paths from the security filter chain entirely.
     * 将WebSocket端点路径完全排除在安全过滤器链之外。
     */
    @Bean
    /**
     * Handles webSecurityCustomizer.
     * ??webSecurityCustomizer?
     */
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/websocket/**");
    }
}
