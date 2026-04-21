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

    @Override
    /**
     * Handles addResourceHandlers.
     * ??addResourceHandlers?
     */
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

