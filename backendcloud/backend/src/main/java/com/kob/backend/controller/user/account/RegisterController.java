package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class RegisterController {
    @Autowired
    private RegisterService registerService;

    /**
     * 创建或保存 register 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of register with controlled input and output handling.
     *
     * @param map 映射参数；Map parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/user/account/register/")
    public Map<String, String> register(@RequestParam Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");
        String confirmedPassword = map.get("confirmedPassword");
        return registerService.register(username, password, confirmedPassword);
    }
}