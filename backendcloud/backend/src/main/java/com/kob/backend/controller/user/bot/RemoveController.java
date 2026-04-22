package com.kob.backend.controller.user.bot;

import com.kob.backend.service.user.bot.RemoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class RemoveController {
    @Autowired
    private RemoveService removeService;

    /**
     * 删除或清理 remove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of remove with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/user/bot/remove/")
    public Map<String, String> remove(@RequestParam Map<String, String> data) {
        return removeService.remove(data);
    }
}