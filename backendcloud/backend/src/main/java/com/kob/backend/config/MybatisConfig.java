package com.kob.backend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类。
 */
@Configuration
public class MybatisConfig {
    /**
     * 处理 mybatisPlusInterceptor 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of mybatisPlusInterceptor with controlled input and output handling.
     *
     * @return 返回 MybatisPlusInterceptor 类型结果；Returns a result of type MybatisPlusInterceptor.
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}