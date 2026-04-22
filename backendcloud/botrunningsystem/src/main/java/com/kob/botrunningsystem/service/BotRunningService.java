package com.kob.botrunningsystem.service;

/**
 * 服务接口定义。
 */
public interface BotRunningService {
    /**
     * 创建或保存 addBot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addBot with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param botCode 机器人相关参数；Bot-related parameter.
     * @param input 输入参数；Input parameter.
     * @param language 语言参数；Language parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    String addBot(Integer userId, String botCode, String input, String language);
}