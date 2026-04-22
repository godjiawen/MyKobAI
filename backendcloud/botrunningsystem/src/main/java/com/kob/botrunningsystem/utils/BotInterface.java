package com.kob.botrunningsystem.utils;

/**
 * 工具类。
 */
public interface BotInterface {
    /**
     * 处理 nextMove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of nextMove with controlled input and output handling.
     *
     * @param input 输入参数；Input parameter.
     * @return 返回数值结果；Returns a numeric result.
     */
    Integer nextMove(String input);
}