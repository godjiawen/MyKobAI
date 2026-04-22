package com.kob.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 配置类。
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig implements Filter {
    /**
     * 处理 doFilter 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of doFilter with controlled input and output handling.
     *
     * @param req 输入参数；Input parameter.
     * @param res 输入参数；Input parameter.
     * @param chain 输入参数；Input parameter.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        if(origin!=null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        String headers = request.getHeader("Access-Control-Request-Headers");
        if(headers!=null) {
            response.setHeader("Access-Control-Allow-Headers", headers);
            response.setHeader("Access-Control-Expose-Headers", headers);
        }

        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 创建或保存 init 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of init with controlled input and output handling.
     *
     * @param filterConfig 输入参数；Input parameter.
     */
    @Override
    public void init(FilterConfig filterConfig) {

    }

    /**
     * 处理 destroy 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of destroy with controlled input and output handling.
     *
     */
    @Override
    public void destroy() {
    }
}