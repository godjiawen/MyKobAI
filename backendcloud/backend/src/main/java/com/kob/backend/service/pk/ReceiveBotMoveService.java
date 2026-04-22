package com.kob.backend.service.pk;

/**
 * 服务接口定义。
 */
public interface ReceiveBotMoveService {
    /**
     * 处理 receiveBotMove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of receiveBotMove with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param direction 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    String receiveBotMove(Integer userId, Integer direction);
}