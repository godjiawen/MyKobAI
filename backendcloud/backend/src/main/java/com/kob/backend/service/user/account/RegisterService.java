package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface RegisterService {
    /**
     * 创建或保存 register 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of register with controlled input and output handling.
     *
     * @param username 用户相关参数；User-related parameter.
     * @param password 密码参数；Password parameter.
     * @param confirmedpassword 密码参数；Password parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    public Map<String, String> register(String username, String password, String confirmedpassword);
}