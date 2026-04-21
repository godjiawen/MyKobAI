package com.kob.backend.service.pk;

import com.kob.backend.consumer.utils.Game;
import com.kob.backend.pojo.GameSnapshot;

/**
 * 对局快照服务接口：管理 Redis 中的对局快照读写。
 */
public interface GameSnapshotService {
    void saveInitial(Game game);
    void saveStep(Game game);
    void savePause(Game game);
    void savePresence(Game game);
    void deleteGame(Game game);
    GameSnapshot getByUserId(Integer userId);
    GameSnapshot getByGameId(String gameId);
}

