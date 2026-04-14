package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface UpdateAccountService {
    Map<String, String> updateUsername(String newUsername);
    Map<String, String> updatePassword(String oldPassword, String newPassword, String confirmedPassword);
}

