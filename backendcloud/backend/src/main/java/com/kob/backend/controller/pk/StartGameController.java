package com.kob.backend.controller.pk;

import com.kob.backend.service.pk.StartGameService;
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
public class StartGameController {
    @Autowired
    private StartGameService startGameService;

    /**
     * 创建或保存 startGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startGame with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @PostMapping("/pk/start/game/")
    public String startGame(@RequestParam MultiValueMap<String, String> data) {
        Integer aId = Integer.parseInt(Objects.requireNonNull(data.getFirst("a_id")));
        Integer aBotId = Integer.parseInt(Objects.requireNonNull(data.getFirst("a_bot_id")));
        Integer bId = Integer.parseInt(Objects.requireNonNull(data.getFirst("b_id")));
        Integer bBotId = Integer.parseInt(Objects.requireNonNull(data.getFirst("b_bot_id")));
        return startGameService.startGame(aId, aBotId, bId, bBotId);
    }
}