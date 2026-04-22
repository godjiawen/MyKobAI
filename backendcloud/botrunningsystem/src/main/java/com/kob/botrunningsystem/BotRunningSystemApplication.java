package com.kob.botrunningsystem;

import com.kob.botrunningsystem.service.impl.BotRunningServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务启动入口。
 */
@SpringBootApplication
public class BotRunningSystemApplication {
    /**
     * 处理 main 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of main with controlled input and output handling.
     *
     * @param args 输入参数；Input parameter.
     */
    public static void main(String[] args) {
        BotRunningServiceImpl.botpool.start();
        SpringApplication.run(BotRunningSystemApplication.class, args);
    }
}