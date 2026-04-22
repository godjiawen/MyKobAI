package com.kob.backend.service.pk;

/**
 * 服务接口定义。
 */
public interface StartGameService {
    /**
     * 创建或保存 startGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startGame with controlled input and output handling.
     *
     * @param aId 标识参数；Identifier value.
     * @param aBotId 标识参数；Identifier value.
     * @param bId 标识参数；Identifier value.
     * @param bBotId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    String startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId);
}