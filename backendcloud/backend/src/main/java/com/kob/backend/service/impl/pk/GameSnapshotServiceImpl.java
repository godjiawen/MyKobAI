package com.kob.backend.service.impl.pk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.Player;
import com.kob.backend.pojo.GameSnapshot;
import com.kob.backend.service.pk.GameSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 快照服务实现。
 * Key 设计：
 *   kob:pk:game:{gameId}:snapshot  -> JSON 快照
 *   kob:pk:user:{userId}:game      -> gameId
 */
@Service
public class GameSnapshotServiceImpl implements GameSnapshotService {

    private static final String KEY_SNAPSHOT = "kob:pk:game:%s:snapshot";
    private static final String KEY_USER_GAME = "kob:pk:user:%s:game";
    private static final long TTL_PLAYING_SEC = 600;   // 10 分钟
    private static final long TTL_SUSPENDED_SEC = 900; // 15 分钟

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ──────────────── private helpers ────────────────

    /**
     * 处理 snapshotKey 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of snapshotKey with controlled input and output handling.
     *
     * @param gameId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    private String snapshotKey(String gameId) {
        return String.format(KEY_SNAPSHOT, gameId);
    }

    /**
     * 处理 userGameKey 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of userGameKey with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    private String userGameKey(Integer userId) {
        return String.format(KEY_USER_GAME, userId);
    }

    /**
     * 构建或转换 resolvePausedBy 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of resolvePausedBy with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    private String resolvePausedBy(Game game) {
        String by = game.getPausedBy();
        if ("A".equals(by)) return String.valueOf(game.getPlayerA().getId());
        if ("B".equals(by)) return String.valueOf(game.getPlayerB().getId());
        return by;
    }

    /**
     * 构建或转换 buildSnapshot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildSnapshot with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    private GameSnapshot buildSnapshot(Game game) {
        Player a = game.getPlayerA();
        Player b = game.getPlayerB();

        GameSnapshot snap = new GameSnapshot();
        snap.setGameId(game.getGameId());
        snap.setStatus("playing");
        snap.setAId(a.getId());
        snap.setBId(b.getId());
        snap.setABotId(a.getBotId());
        snap.setBBotId(b.getBotId());
        snap.setASx(a.getSx());
        snap.setASy(a.getSy());
        snap.setBSx(b.getSx());
        snap.setBSy(b.getSy());
        snap.setMap(game.getG());
        snap.setASteps(a.getStepsString());
        snap.setBSteps(b.getStepsString());
        snap.setPaused(game.isPaused());
        snap.setPausedBy(resolvePausedBy(game));
        snap.setSuspended(game.isSuspended());
        snap.setSuspendedBy(game.getSuspendedBy());
        snap.setSuspendedReason(game.getSuspendedReason());
        List<Integer> away = new ArrayList<>(game.getAwayPlayers());
        snap.setAwayUserIds(away);
        snap.setRoomId(game.getRoomId());
        snap.setLoser("");
        snap.setUpdatedAt(System.currentTimeMillis());
        long ttl = game.isSuspended() ? TTL_SUSPENDED_SEC : TTL_PLAYING_SEC;
        snap.setExpireAt(System.currentTimeMillis() + ttl * 1000);
        return snap;
    }

    /**
     * 处理 writeSnapshot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of writeSnapshot with controlled input and output handling.
     *
     * @param snap 输入参数；Input parameter.
     */
    private void writeSnapshot(GameSnapshot snap) {
        try {
            String gameId = snap.getGameId();
            long ttl = snap.isSuspended() ? TTL_SUSPENDED_SEC : TTL_PLAYING_SEC;
            String json = JSON.toJSONString(snap);

            redisTemplate.opsForValue().set(snapshotKey(gameId), json, ttl, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(userGameKey(snap.getAId()), gameId, ttl, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(userGameKey(snap.getBId()), gameId, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[Redis] writeSnapshot failed: " + e.getMessage());
        }
    }

    // ──────────────── public API ────────────────

    /**
     * 创建或保存 saveInitial 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of saveInitial with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    @Override
    public void saveInitial(Game game) {
        writeSnapshot(buildSnapshot(game));
    }

    /**
     * 创建或保存 saveStep 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of saveStep with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    @Override
    public void saveStep(Game game) {
        writeSnapshot(buildSnapshot(game));
    }

    /**
     * 创建或保存 savePause 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of savePause with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    @Override
    public void savePause(Game game) {
        writeSnapshot(buildSnapshot(game));
    }

    /**
     * 创建或保存 savePresence 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of savePresence with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    @Override
    public void savePresence(Game game) {
        writeSnapshot(buildSnapshot(game));
    }

    /**
     * 删除或清理 deleteGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of deleteGame with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    @Override
    public void deleteGame(Game game) {
        try {
            String gameId = game.getGameId();
            redisTemplate.delete(snapshotKey(gameId));
            redisTemplate.delete(userGameKey(game.getPlayerA().getId()));
            redisTemplate.delete(userGameKey(game.getPlayerB().getId()));
        } catch (Exception e) {
            System.err.println("[Redis] deleteGame failed: " + e.getMessage());
        }
    }

    /**
     * 查询并返回 getByUserId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getByUserId with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    @Override
    public GameSnapshot getByUserId(Integer userId) {
        try {
            String gameId = redisTemplate.opsForValue().get(userGameKey(userId));
            if (gameId == null) return null;
            return getByGameId(gameId);
        } catch (Exception e) {
            System.err.println("[Redis] getByUserId failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 查询并返回 getByGameId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getByGameId with controlled input and output handling.
     *
     * @param gameId 标识参数；Identifier value.
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    @Override
    public GameSnapshot getByGameId(String gameId) {
        try {
            String json = redisTemplate.opsForValue().get(snapshotKey(gameId));
            if (json == null) return null;
            return JSON.parseObject(json, GameSnapshot.class);
        } catch (Exception e) {
            System.err.println("[Redis] getByGameId failed: " + e.getMessage());
            return null;
        }
    }
}