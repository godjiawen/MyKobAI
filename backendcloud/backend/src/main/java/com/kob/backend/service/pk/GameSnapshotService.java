package com.kob.backend.service.pk;

import com.kob.backend.consumer.utils.Game;
import com.kob.backend.pojo.GameSnapshot;

/**
 * 对局快照服务接口：管理 Redis 中的对局快照读写。
 */
public interface GameSnapshotService {
    /**
     * 创建或保存 saveInitial 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of saveInitial with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    void saveInitial(Game game);
    /**
     * 创建或保存 saveStep 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of saveStep with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    void saveStep(Game game);
    /**
     * 创建或保存 savePause 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of savePause with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    void savePause(Game game);
    /**
     * 创建或保存 savePresence 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of savePresence with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    void savePresence(Game game);
    /**
     * 删除或清理 deleteGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of deleteGame with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    void deleteGame(Game game);
    /**
     * 查询并返回 getByUserId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getByUserId with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    GameSnapshot getByUserId(Integer userId);
    /**
     * 查询并返回 getByGameId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getByGameId with controlled input and output handling.
     *
     * @param gameId 标识参数；Identifier value.
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    GameSnapshot getByGameId(String gameId);
}