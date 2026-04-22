package com.kob.backend.service.user.account;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface InfoService {
    /**
     * 查询并返回 getinfo 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getinfo with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, String> getinfo();
}