package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface LoginService {
    public Map<String, String> getToken(String username, String password);
}
