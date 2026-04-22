package com.kob.matchingsystem.service;

/**
 * 服务接口定义。
 */
public interface MatchingService {
    /**
     * 创建或保存 addPlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addPlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param rating 输入参数；Input parameter.
     * @param botId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    String addPlayer(Integer userId, Integer rating, Integer botId);
    /**
     * 删除或清理 removePlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of removePlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    String removePlayer(Integer userId);
}