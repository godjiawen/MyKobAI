package com.kob.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务启动入口。
 */
@SpringBootApplication
public class BackendApplication {

    /**
     * 处理 main 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of main with controlled input and output handling.
     *
     * @param args 输入参数；Input parameter.
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}