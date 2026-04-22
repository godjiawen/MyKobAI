package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.UpdateAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class UpdateAccountController {

    @Autowired
    private UpdateAccountService updateAccountService;

    /**
     * 更新 updateUsername 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of updateUsername with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/user/account/update/username/")
    public Map<String, String> updateUsername(@RequestParam Map<String, String> data) {
        String newUsername = data.get("new_username");
        return updateAccountService.updateUsername(newUsername);
    }

    /**
     * 更新 updatePassword 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of updatePassword with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/user/account/update/password/")
    public Map<String, String> updatePassword(@RequestParam Map<String, String> data) {
        String oldPassword = data.get("old_password");
        String newPassword = data.get("new_password");
        String confirmedPassword = data.get("confirmed_password");
        return updateAccountService.updatePassword(oldPassword, newPassword, confirmedPassword);
    }
}