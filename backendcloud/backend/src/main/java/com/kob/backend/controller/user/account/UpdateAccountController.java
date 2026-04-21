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

    @PostMapping("/api/user/account/update/username/")
    /**
     * Handles updateUsername.
     * ??updateUsername?
     */
    public Map<String, String> updateUsername(@RequestParam Map<String, String> data) {
        String newUsername = data.get("new_username");
        return updateAccountService.updateUsername(newUsername);
    }

    @PostMapping("/api/user/account/update/password/")
    /**
     * Handles updatePassword.
     * ??updatePassword?
     */
    public Map<String, String> updatePassword(@RequestParam Map<String, String> data) {
        String oldPassword = data.get("old_password");
        String newPassword = data.get("new_password");
        String confirmedPassword = data.get("confirmed_password");
        return updateAccountService.updatePassword(oldPassword, newPassword, confirmedPassword);
    }
}

