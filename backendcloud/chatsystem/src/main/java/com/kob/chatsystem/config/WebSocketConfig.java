package com.kob.chatsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 配置类。
 */
@Configuration
public class WebSocketConfig {
    /**
     * 处理 serverEndpointExporter 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of serverEndpointExporter with controlled input and output handling.
     *
     * @return 返回 ServerEndpointExporter 类型结果；Returns a result of type ServerEndpointExporter.
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
