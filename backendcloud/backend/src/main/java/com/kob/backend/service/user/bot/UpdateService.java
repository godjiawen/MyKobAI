package com.kob.backend.service.user.bot;

import java.util.Map;

/**
 * 服务接口定义。
 */
public interface UpdateService {
    /**
     * 更新 update 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of update with controlled input and output handling.
     *
     * @param data 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, String> update(Map<String, String> data);
}