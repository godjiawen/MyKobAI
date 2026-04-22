package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.GameSnapshot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.pk.GameSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server endpoint that manages player connections, matching, game sessions and real-time messaging.
 * WebSocket鏈嶅姟绔紝绠＄悊鐜╁杩炴帴銆佸尮閰嶃€佹父鎴忎細璇濆強瀹炴椂娑堟伅鏀跺彂銆?
 */
@Component
@ServerEndpoint("/websocket/{token}")
public class WebSocketServer {

    public static final ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();

    private User user;
    private Session session = null;

    public static UserMapper userMapper;
    public static RecordMapper recordMapper;
    private static BotMapper botMapper;
    public static RestTemplate restTemplate;
    private static GameSnapshotService gameSnapshotService;
    public Game game = null;
    private static final String addPlayerUrl = "http://127.0.0.1:3001/player/add/";
    private static final String removePlayerUrl = "http://127.0.0.1:3001/player/remove/";

    /**
     * 更新 setUserMapper 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setUserMapper with controlled input and output handling.
     *
     * @param userMapper 用户相关参数；User-related parameter.
     */
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    /**
     * 更新 setRecordMapper 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setRecordMapper with controlled input and output handling.
     *
     * @param recordMapper 映射参数；Map parameter.
     */
    @Autowired
    public void setRecordMapper(RecordMapper recordMapper) {
        WebSocketServer.recordMapper = recordMapper;
    }

    /**
     * 更新 setBotMapper 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setBotMapper with controlled input and output handling.
     *
     * @param botMapper 机器人相关参数；Bot-related parameter.
     */
    @Autowired
    public void setBotMapper(BotMapper botMapper) {
        WebSocketServer.botMapper = botMapper;
    }

    /**
     * 更新 setRestTemplate 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setRestTemplate with controlled input and output handling.
     *
     * @param restTemplate 输入参数；Input parameter.
     */
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        WebSocketServer.restTemplate = restTemplate;
    }

    /**
     * 更新 setGameSnapshotService 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setGameSnapshotService with controlled input and output handling.
     *
     * @param gameSnapshotService 对局相关参数；Game-related parameter.
     */
    @Autowired
    public void setGameSnapshotService(GameSnapshotService gameSnapshotService) {
        WebSocketServer.gameSnapshotService = gameSnapshotService;
        Game.gameSnapshotService = gameSnapshotService;
    }

    /**
     * 处理 PathParam 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of PathParam with controlled input and output handling.
     *
     * @param session 输入参数；Input parameter.
     * @param token 令牌参数；Token parameter.
     * @return 返回 void onOpen(Session session, @ 类型结果；Returns a result of type void onOpen(Session session, @.
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        this.session = session;
        System.out.println("connected!");

        Integer userId;
        try {
            userId = JwtAuthentication.getUserId(token);
        } catch (Exception e) {
            this.session.close();
            return;
        }

        this.user = userMapper.selectById(userId);
        if (this.user != null) {
            users.put(userId, this);
            // 閲嶈繛鏃惰嚜鍔ㄦ煡璇?Redis锛屾仮澶嶈繘琛屼腑瀵瑰眬
            autoSyncGame(userId);
        } else {
            this.session.close();
        }
        System.out.println(users);
    }

    /**
     * 处理 onClose 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of onClose with controlled input and output handling.
     *
     */
    @OnClose
    public void onClose() {
        System.out.println("disconnected!");
        if (this.user != null) {
            users.remove(this.user.getId());
            // 鏂嚎绛変环浜?game-leave锛屼笉娓呯┖ game 寮曠敤锛屾敼涓烘爣璁版殏绂?
            if (this.game != null) {
                markPlayerLeave(this.game, this.user.getId(), "disconnect");
            }
        }
    }

    /**
     * 创建或保存 startGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startGame with controlled input and output handling.
     *
     * @param aId 标识参数；Identifier value.
     * @param aBotId 标识参数；Identifier value.
     * @param bId 标识参数；Identifier value.
     * @param bBotId 标识参数；Identifier value.
     */
    public static void startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId) {
        User a = userMapper.selectById(aId);
        User b = userMapper.selectById(bId);
        Bot botA = botMapper.selectById(aBotId);
        Bot botB = botMapper.selectById(bBotId);

        String roomId = Math.min(aId, bId) + "_" + Math.max(aId, bId);
        String gameId = "game_" + aId + "_" + bId + "_" + System.currentTimeMillis();

        Game game = new Game(13, 14, 20, a.getId(), botA, b.getId(), botB, gameId, roomId);
        game.createMap();
        activeGames.put(gameId, game);
        if (users.get(a.getId()) != null) users.get(a.getId()).game = game;
        if (users.get(b.getId()) != null) users.get(b.getId()).game = game;

        // 鍐欏叆 Redis 鍒濆蹇収
        if (gameSnapshotService != null) {
            try { gameSnapshotService.saveInitial(game); } catch (Exception e) {
                System.err.println("[Redis] saveInitial failed: " + e.getMessage());
            }
        }

        game.start();

        JSONObject respGame = new JSONObject();
        respGame.put("a_id", game.getPlayerA().getId());
        respGame.put("b_id", game.getPlayerB().getId());
        respGame.put("a_sx", game.getPlayerA().getSx());
        respGame.put("b_sx", game.getPlayerB().getSx());
        respGame.put("a_sy", game.getPlayerA().getSy());
        respGame.put("b_sy", game.getPlayerB().getSy());
        respGame.put("map", game.getG());


        JSONObject respA = new JSONObject();
        respA.put("event", "start-matching");
        respA.put("opponent_username", b.getUsername());
        respA.put("opponent_photo", b.getPhoto());
        respA.put("game", respGame);
        respA.put("room_id", roomId);
        sendEvent(a.getId(), respA);

        JSONObject respB = new JSONObject();
        respB.put("event", "start-matching");
        respB.put("opponent_username", a.getUsername());
        respB.put("opponent_photo", a.getPhoto());
        respB.put("game", respGame);
        respB.put("room_id", roomId);
        sendEvent(b.getId(), respB);
    }

    /**
     * 校验或判断 isUserOnline 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isUserOnline with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    public static boolean isUserOnline(Integer userId) {
        return userId != null && users.containsKey(userId);
    }

    /**
     * 校验或判断 isUserInGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isUserInGame with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    public static boolean isUserInGame(Integer userId) {
        if (userId == null) return false;
        WebSocketServer server = users.get(userId);
        return server != null && server.game != null;
    }

    /**
     * 查询并返回 getUserOnlineStatus 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getUserOnlineStatus with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    public static String getUserOnlineStatus(Integer userId) {
        if (!isUserOnline(userId)) return "offline";
        if (isUserInGame(userId)) return "in_game";
        return "online";
    }

    /**
     * 删除或清理 clearGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of clearGame with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     */
    public static void clearGame(Integer userId) {
        WebSocketServer server = users.get(userId);
        if (server != null) {
            if (server.game != null) {
                activeGames.remove(server.game.getGameId(), server.game);
            }
            server.game = null;
        }
    }

    /**
     * 删除或清理 clearGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of clearGame with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     */
    public static void clearGame(Game game) {
        if (game == null) return;
        activeGames.remove(game.getGameId(), game);
        clearGame(game.getPlayerA().getId());
        clearGame(game.getPlayerB().getId());
    }

    /**
     * 删除或清理 clearGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of clearGame with controlled input and output handling.
     *
     * @param leftUserId 标识参数；Identifier value.
     * @param rightUserId 标识参数；Identifier value.
     */
    public static void clearGame(Integer leftUserId, Integer rightUserId) {
        clearGame(leftUserId);
        clearGame(rightUserId);
    }

    /**
     * 发送或通知 sendEvent 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendEvent with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param event 输入参数；Input parameter.
     */
    public static void sendEvent(Integer userId, JSONObject event) {
        if (userId == null || event == null) return;
        WebSocketServer server = users.get(userId);
        if (server != null) {
            server.sendMessage(event.toJSONString());
        }
    }

    /**
     * 创建或保存 startMatching 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startMatching with controlled input and output handling.
     *
     * @param botId 标识参数；Identifier value.
     */
    private void startMatching(Integer botId) {
        System.out.println("start matching!");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        data.add("rating", this.user.getRating().toString());
        data.add("bot_id", botId.toString());
        restTemplate.postForObject(addPlayerUrl, data, String.class);
    }

    /**
     * 删除或清理 stopMatching 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of stopMatching with controlled input and output handling.
     *
     */
    private void stopMatching() {
        System.out.println("stop matching");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    /**
     * 处理 move 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of move with controlled input and output handling.
     *
     * @param direction 输入参数；Input parameter.
     */
    private void move(int direction) {
        if (game == null) return;
        if (game.isPaused()) return;
        if (game.getPlayerA().getId().equals(user.getId())) {
            if (game.getPlayerA().getBotId().equals(-1)) game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(user.getId())) {
            if (game.getPlayerB().getBotId().equals(-1)) game.setNextStepB(direction);
        }
    }

    /**
     * 处理 broadcastToGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of broadcastToGame with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     */
    private void broadcastToGame(String message) {
        Integer aId = game.getPlayerA().getId();
        Integer bId = game.getPlayerB().getId();
        if (users.get(aId) != null) users.get(aId).sendMessage(message);
        if (users.get(bId) != null) users.get(bId).sendMessage(message);
    }

    /**
     * 处理 pause 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of pause with controlled input and output handling.
     *
     */
    private void pause() {
        if (game == null) return;
        if (game.isPaused()) return;

        String by = user.getId().toString();
        game.setPaused(true, by);

        if (gameSnapshotService != null) {
            try { gameSnapshotService.savePause(game); } catch (Exception e) {
                System.err.println("[Redis] savePause failed: " + e.getMessage());
            }
        }

        JSONObject resp = new JSONObject();
        resp.put("event", "game-paused");
        resp.put("paused_by", user.getId());
        broadcastToGame(resp.toJSONString());
    }

    /**
     * 处理 resume 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of resume with controlled input and output handling.
     *
     */
    private void resume() {
        if (game == null) return;
        if (!game.isPaused()) return;

        String by = user.getId().toString();
        if (!by.equals(game.getPausedBy())) return;

        game.setPaused(false, "");

        if (gameSnapshotService != null) {
            try { gameSnapshotService.savePause(game); } catch (Exception e) {
                System.err.println("[Redis] savePause failed: " + e.getMessage());
            }
        }

        JSONObject resp = new JSONObject();
        resp.put("event", "game-resumed");
        broadcastToGame(resp.toJSONString());
    }

    /**
     * 构建或转换 buildSnapshotJSON 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildSnapshotJSON with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     * @return 返回 JSONObject 类型结果；Returns a result of type JSONObject.
     */
    private static JSONObject buildSnapshotJSON(Game game) {
        JSONObject snap = new JSONObject();
        snap.put("game_id", game.getGameId());
        snap.put("status", "playing");
        snap.put("a_id", game.getPlayerA().getId());
        snap.put("b_id", game.getPlayerB().getId());
        snap.put("a_bot_id", game.getPlayerA().getBotId());
        snap.put("b_bot_id", game.getPlayerB().getBotId());
        snap.put("a_sx", game.getPlayerA().getSx());
        snap.put("a_sy", game.getPlayerA().getSy());
        snap.put("b_sx", game.getPlayerB().getSx());
        snap.put("b_sy", game.getPlayerB().getSy());
        snap.put("map", game.getG());
        snap.put("a_steps", game.getPlayerA().getStepsString());
        snap.put("b_steps", game.getPlayerB().getStepsString());
        snap.put("paused", game.isPaused());
        String pausedBy = game.getPausedBy();
        if ("A".equals(pausedBy)) pausedBy = String.valueOf(game.getPlayerA().getId());
        if ("B".equals(pausedBy)) pausedBy = String.valueOf(game.getPlayerB().getId());
        snap.put("paused_by", pausedBy);
        snap.put("suspended", game.isSuspended());
        snap.put("suspended_by", game.getSuspendedBy());
        snap.put("suspended_reason", game.getSuspendedReason());
        snap.put("away_user_ids", new java.util.ArrayList<>(game.getAwayPlayers()));
        snap.put("room_id", game.getRoomId());
        snap.put("loser", "");
        snap.put("updated_at", System.currentTimeMillis());
        return snap;
    }

    /**
     * 查询并返回 loadSnapshotForCurrentUser 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of loadSnapshotForCurrentUser with controlled input and output handling.
     *
     * @return 返回 GameSnapshot 类型结果；Returns a result of type GameSnapshot.
     */
    private GameSnapshot loadSnapshotForCurrentUser() {
        if (user == null || gameSnapshotService == null) return null;
        try {
            return gameSnapshotService.getByUserId(user.getId());
        } catch (Exception e) {
            System.err.println("[Redis] loadSnapshotForCurrentUser failed: " + e.getMessage());
            return null;
        }
    }
    /**
     * 处理 bindGameFromSnapshot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of bindGameFromSnapshot with controlled input and output handling.
     *
     * @param snap 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean bindGameFromSnapshot(GameSnapshot snap) {
        if (snap == null || user == null || this.game != null) return this.game != null;
        Game active = activeGames.get(snap.getGameId());
        if (active == null) return false;
        Integer userId = user.getId();
        if (!userId.equals(active.getPlayerA().getId()) && !userId.equals(active.getPlayerB().getId())) {
            return false;
        }
        this.game = active;
        return true;
    }

    /**
     * 处理 syncGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of syncGame with controlled input and output handling.
     *
     */
    private void syncGame() {
        if (user == null) return;
        // Prefer in-memory game state to avoid stale snapshot gaps.
        if (this.game != null) {
            JSONObject resp = new JSONObject();
            resp.put("event", "game-resync");
            resp.put("snapshot", buildSnapshotJSON(this.game));
            sendMessage(resp.toJSONString());
            return;
        }
        GameSnapshot snap = loadSnapshotForCurrentUser();
        if (bindGameFromSnapshot(snap) && this.game != null) {
            JSONObject resp = new JSONObject();
            resp.put("event", "game-resync");
            resp.put("snapshot", buildSnapshotJSON(this.game));
            sendMessage(resp.toJSONString());
            return;
        }
        Integer uid = user.getId();
        if (snap != null && (uid.equals(snap.getAId()) || uid.equals(snap.getBId()))) {
            JSONObject resp = new JSONObject();
            resp.put("event", "game-resync");
            // Build snake_case payload explicitly to match frontend expectations.
            JSONObject snapJson = new JSONObject();
            snapJson.put("game_id", snap.getGameId());
            snapJson.put("status", snap.getStatus());
            snapJson.put("a_id", snap.getAId());
            snapJson.put("b_id", snap.getBId());
            snapJson.put("a_bot_id", snap.getABotId());
            snapJson.put("b_bot_id", snap.getBBotId());
            snapJson.put("a_sx", snap.getASx());
            snapJson.put("a_sy", snap.getASy());
            snapJson.put("b_sx", snap.getBSx());
            snapJson.put("b_sy", snap.getBSy());
            snapJson.put("map", snap.getMap());
            snapJson.put("a_steps", snap.getASteps());
            snapJson.put("b_steps", snap.getBSteps());
            snapJson.put("paused", snap.isPaused());
            snapJson.put("paused_by", snap.getPausedBy());
            snapJson.put("suspended", snap.isSuspended());
            snapJson.put("suspended_by", snap.getSuspendedBy());
            snapJson.put("suspended_reason", snap.getSuspendedReason());
            snapJson.put("away_user_ids", snap.getAwayUserIds());
            snapJson.put("room_id", snap.getRoomId());
            snapJson.put("loser", snap.getLoser());
            snapJson.put("updated_at", snap.getUpdatedAt());
            resp.put("snapshot", snapJson);
            sendMessage(resp.toJSONString());
            return;
        }
        JSONObject resp = new JSONObject();
        resp.put("event", "no-active-game");
        sendMessage(resp.toJSONString());
    }

    /**
     * 处理 gameEnter 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of gameEnter with controlled input and output handling.
     *
     */
    private void gameEnter() {
        if (game == null) {
            bindGameFromSnapshot(loadSnapshotForCurrentUser());
            if (game == null) {
                syncGame();
                return;
            }
        }
        Integer userId = user.getId();
        game.removeAwayPlayer(userId);
        if (gameSnapshotService != null) {
            try {
                gameSnapshotService.savePresence(game);
            } catch (Exception e) {
                System.err.println("[Redis] savePresence failed: " + e.getMessage());
            }
        }
        syncGame();
        Integer opponentId = game.getPlayerA().getId().equals(userId)
                ? game.getPlayerB().getId() : game.getPlayerA().getId();
        JSONObject back = new JSONObject();
        back.put("event", "player-back");
        back.put("user_id", userId);
        sendEvent(opponentId, back);
        if (game.getAwayPlayers().isEmpty() && !game.isPaused()) {
            game.setSuspended(false, null, "");
            if (gameSnapshotService != null) {
                try {
                    gameSnapshotService.savePresence(game);
                } catch (Exception e) {
                    System.err.println("[Redis] savePresence failed: " + e.getMessage());
                }
            }
            JSONObject resumed = new JSONObject();
            resumed.put("event", "game-resumed-from-away");
            broadcastToGame(resumed.toJSONString());
        }
    }

    /**
     * 处理 markPlayerLeave 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of markPlayerLeave with controlled input and output handling.
     *
     * @param game 对局相关参数；Game-related parameter.
     * @param userId 标识参数；Identifier value.
     * @param reason 输入参数；Input parameter.
     */
    private static void markPlayerLeave(Game game, Integer userId, String reason) {
        game.addAwayPlayer(userId);
        game.setSuspended(true, userId, reason);

        if (gameSnapshotService != null) {
            try { gameSnapshotService.savePresence(game); } catch (Exception e) {
                System.err.println("[Redis] savePresence failed: " + e.getMessage());
            }
        }

        // 骞挎挱
        Integer aId = game.getPlayerA().getId();
        Integer bId = game.getPlayerB().getId();
        Integer opponentId = aId.equals(userId) ? bId : aId;

        JSONObject away = new JSONObject();
        away.put("event", "player-away");
        away.put("user_id", userId);
        away.put("reason", reason);
        sendEvent(opponentId, away);

        JSONObject suspended = new JSONObject();
        suspended.put("event", "game-suspended");
        suspended.put("suspended_by", userId);
        suspended.put("reason", reason);
        sendEvent(aId, suspended);
        sendEvent(bId, suspended);
    }

    /**
     * 处理 gameLeave 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of gameLeave with controlled input and output handling.
     *
     * @param reason 输入参数；Input parameter.
     */
    private void gameLeave(String reason) {
        if (game == null) return;
        markPlayerLeave(game, user.getId(), reason);
    }

    /**
     * 处理 autoSyncGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of autoSyncGame with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     */
    private void autoSyncGame(Integer userId) {
        syncGame();
    }

    /**
     * 处理 onMessage 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of onMessage with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     * @param session 输入参数；Input parameter.
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("receive message!");
        JSONObject data = JSONObject.parseObject(message);
        String event = data.getString("event");
        if ("start-matching".equals(event)) {
            startMatching(data.getInteger("bot_id"));
        } else if ("stop-matching".equals(event)) {
            stopMatching();
        } else if ("move".equals(event)) {
            move(data.getInteger("direction"));
        } else if ("pause".equals(event)) {
            pause();
        } else if ("resume".equals(event)) {
            resume();
        } else if ("sync-game".equals(event)) {
            syncGame();
        } else if ("game-enter".equals(event)) {
            gameEnter();
        } else if ("game-leave".equals(event)) {
            gameLeave(data.getString("reason") != null ? data.getString("reason") : "route-leave");
        }
    }

    /**
     * 处理 onError 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of onError with controlled input and output handling.
     *
     * @param session 输入参数；Input parameter.
     * @param error 输入参数；Input parameter.
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /**
     * 发送或通知 sendMessage 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendMessage with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     */
    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

