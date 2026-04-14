package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface RegisterService {
    public Map<String, String> register(String username, String password, String confirmedpassword);
}
