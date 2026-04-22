package com.kob.matchingsystem;

import com.kob.matchingsystem.service.impl.MatchingServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MatchingSystemApplication {
    /**
     * 处理 main 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of main with controlled input and output handling.
     *
     * @param args 输入参数；Input parameter.
     */
    public static void main(String[] args) {
        MatchingServiceImpl.matchingPool.start(); //启动匹配线程
        SpringApplication.run(MatchingSystemApplication.class, args);
    }
}