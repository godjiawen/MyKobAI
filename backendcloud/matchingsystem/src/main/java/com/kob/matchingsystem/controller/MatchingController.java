package com.kob.matchingsystem.controller;

import com.kob.matchingsystem.service.MatchingService;
import com.kob.matchingsystem.service.impl.utils.MatchingPool;
import com.kob.matchingsystem.service.impl.utils.Player;
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
public class MatchingController {
    @Autowired
    private MatchingService matchingService;

    /**
     * 创建或保存 addPlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addPlayer with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @PostMapping("/player/add/")
    public String addPlayer(@RequestParam MultiValueMap<String, String> data) {
        Integer userId = Integer.parseInt(Objects.requireNonNull(data.getFirst("user_id")));
        Integer rating = Integer.parseInt(Objects.requireNonNull(data.getFirst("rating")));
        Integer botId = Integer.parseInt(Objects.requireNonNull(data.getFirst("bot_id")));
        return matchingService.addPlayer(userId, rating, botId);
    }

    /**
     * 删除或清理 removePlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of removePlayer with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @PostMapping("/player/remove/")
    public String removePlayer(@RequestParam MultiValueMap<String, String> data) {
        Integer userId = Integer.parseInt(Objects.requireNonNull(data.getFirst("user_id")));
        return matchingService.removePlayer(userId);
    }
}
