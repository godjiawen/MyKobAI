package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface UpdateAccountService {
    /**
     * 更新 updateUsername 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of updateUsername with controlled input and output handling.
     *
     * @param newUsername 用户相关参数；User-related parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, String> updateUsername(String newUsername);
    /**
     * 更新 updatePassword 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of updatePassword with controlled input and output handling.
     *
     * @param oldPassword 密码参数；Password parameter.
     * @param newPassword 密码参数；Password parameter.
     * @param confirmedPassword 密码参数；Password parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, String> updatePassword(String oldPassword, String newPassword, String confirmedPassword);
}