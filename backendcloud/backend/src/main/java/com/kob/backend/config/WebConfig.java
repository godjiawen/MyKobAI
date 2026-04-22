package com.kob.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${kob.upload.dir}")
    private String uploadDir;

    /**
     * 创建或保存 addResourceHandlers 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addResourceHandlers with controlled input and output handling.
     *
     * @param registry 输入参数；Input parameter.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 映射本地头像目录，使外部请求可直接访问头像资源
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + uploadDir);
    }
}