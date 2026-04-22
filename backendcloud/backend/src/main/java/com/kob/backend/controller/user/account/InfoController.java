package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class InfoController {
    @Autowired
    private InfoService infoService;

    /**
     * 查询并返回 getinfo 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getinfo with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @GetMapping("/api/user/account/info/")
    public Map<String, String> getinfo() {
        return infoService.getinfo();
    }
}