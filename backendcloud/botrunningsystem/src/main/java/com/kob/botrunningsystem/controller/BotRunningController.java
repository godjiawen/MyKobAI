package com.kob.botrunningsystem.controller;


import com.kob.botrunningsystem.service.BotRunningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class BotRunningController {
    @Autowired
    private BotRunningService botRunningService;

    /**
     * 创建或保存 addBot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addBot with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @PostMapping("/bot/add/")
    public String addBot(@RequestParam MultiValueMap<String, String> data) {
        Integer userId = Integer.parseInt(Objects.requireNonNull(data.getFirst("user_id")));
        String botCode = data.getFirst("bot_code");
        String input = data.getFirst("input");
        String language = data.getOrDefault("language", java.util.List.of("java")).get(0);
        return botRunningService.addBot(userId, botCode, input, language);
    }
}