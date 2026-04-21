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
     * Injects UserMapper as a static dependency for shared access.
     * 娉ㄥ叆UserMapper浣滀负闈欐€佷緷璧栦互渚垮叡浜闂€?
     */
    @Autowired
    /**
     * Handles setUserMapper.
     * ??setUserMapper?
     */
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    /**
     * Injects RecordMapper as a static dependency for shared access.
     * 娉ㄥ叆RecordMapper浣滀负闈欐€佷緷璧栦互渚垮叡浜闂€?
     */
    @Autowired
    /**
     * Handles setRecordMapper.
     * ??setRecordMapper?
     */
    public void setRecordMapper(RecordMapper recordMapper) {
        WebSocketServer.recordMapper = recordMapper;
    }

    /**
     * Injects BotMapper as a static dependency for shared access.
     * 娉ㄥ叆BotMapper浣滀负闈欐€佷緷璧栦互渚垮叡浜闂€?
     */
    @Autowired
    /**
     * Handles setBotMapper.
     * ??setBotMapper?
     */
    public void setBotMapper(BotMapper botMapper) {
        WebSocketServer.botMapper = botMapper;
    }

    /**
     * Injects RestTemplate as a static dependency for shared access.
     * 娉ㄥ叆RestTemplate浣滀负闈欐€佷緷璧栦互渚垮叡浜闂€?
     */
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        WebSocketServer.restTemplate = restTemplate;
    }

    @Autowired
    public void setGameSnapshotService(GameSnapshotService gameSnapshotService) {
        WebSocketServer.gameSnapshotService = gameSnapshotService;
        Game.gameSnapshotService = gameSnapshotService;
    }

    /**
     * Called when a new WebSocket connection is established; validates token and registers user.
     * 寤虹珛鏂癢ebSocket杩炴帴鏃惰皟鐢紱楠岃瘉token骞舵敞鍐岀敤鎴枫€?
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
     * Called when the WebSocket connection is closed; removes user from the active users map.
     * WebSocket杩炴帴鍏抽棴鏃惰皟鐢紱浠庡湪绾跨敤鎴锋槧灏勪腑绉婚櫎璇ョ敤鎴枫€?
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
     * Creates and starts a game between two matched players, then notifies both via WebSocket.
     * 涓轰袱鍚嶅尮閰嶇殑鐜╁鍒涘缓骞跺惎鍔ㄦ父鎴忥紝鐒跺悗閫氳繃WebSocket閫氱煡鍙屾柟銆?
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
     * Returns whether the user with the given ID is currently connected.
     * 杩斿洖鎸囧畾ID鐨勭敤鎴峰綋鍓嶆槸鍚﹀凡杩炴帴銆?
     */
    public static boolean isUserOnline(Integer userId) {
        return userId != null && users.containsKey(userId);
    }

    /**
     * Returns whether the user with the given ID is currently in a game.
     * 杩斿洖鎸囧畾ID鐨勭敤鎴峰綋鍓嶆槸鍚﹀湪娓告垙涓€?
     */
    public static boolean isUserInGame(Integer userId) {
        if (userId == null) return false;
        WebSocketServer server = users.get(userId);
        return server != null && server.game != null;
    }

    /**
     * Returns the online status string of a user: "offline", "in_game" or "online".
     * 杩斿洖鐢ㄦ埛鐨勫湪绾跨姸鎬佸瓧绗︿覆锛?offline"銆?in_game"鎴?online"銆?
     */
    public static String getUserOnlineStatus(Integer userId) {
        if (!isUserOnline(userId)) return "offline";
        if (isUserInGame(userId)) return "in_game";
        return "online";
    }

    /**
     * Clears the game reference for a single user.
     * 娓呴櫎鍗曚釜鐢ㄦ埛鐨勬父鎴忓紩鐢ㄣ€?
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

    public static void clearGame(Game game) {
        if (game == null) return;
        activeGames.remove(game.getGameId(), game);
        clearGame(game.getPlayerA().getId());
        clearGame(game.getPlayerB().getId());
    }

    /**
     * Clears the game reference for both players after a game ends.
     * 娓告垙缁撴潫鍚庢竻闄ゅ弻鏂圭帺瀹剁殑娓告垙寮曠敤銆?
     */
    public static void clearGame(Integer leftUserId, Integer rightUserId) {
        clearGame(leftUserId);
        clearGame(rightUserId);
    }

    /**
     * Sends a JSON event message to a specific user via WebSocket.
     * 閫氳繃WebSocket鍚戠壒瀹氱敤鎴峰彂閫丣SON浜嬩欢娑堟伅銆?
     */
    public static void sendEvent(Integer userId, JSONObject event) {
        if (userId == null || event == null) return;
        WebSocketServer server = users.get(userId);
        if (server != null) {
            server.sendMessage(event.toJSONString());
        }
    }

    /**
     * Adds the current user to the matching queue with the specified bot ID.
     * 灏嗗綋鍓嶇敤鎴蜂互鎸囧畾鏈哄櫒浜篒D鍔犲叆鍖归厤闃熷垪銆?
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
     * Removes the current user from the matching queue.
     * 灏嗗綋鍓嶇敤鎴蜂粠鍖归厤闃熷垪涓Щ闄ゃ€?
     */
    private void stopMatching() {
        System.out.println("stop matching");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    /**
     * Processes a move direction input from a human player during a game.
     * 澶勭悊娓告垙涓汉绫荤帺瀹惰緭鍏ョ殑绉诲姩鏂瑰悜銆?
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
     * Broadcasts a message to both players in the current game.
     * 鍚戝綋鍓嶆父鎴忎腑鐨勫弻鏂圭帺瀹跺箍鎾秷鎭€?
     */
    private void broadcastToGame(String message) {
        Integer aId = game.getPlayerA().getId();
        Integer bId = game.getPlayerB().getId();
        if (users.get(aId) != null) users.get(aId).sendMessage(message);
        if (users.get(bId) != null) users.get(bId).sendMessage(message);
    }

    /**
     * Pauses the current game and notifies both players who triggered the pause.
     * 鏆傚仠褰撳墠娓告垙骞堕€氱煡鍙屾柟鐜╁鏆傚仠瑙﹀彂鏂广€?
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
     * Resumes the current game if the requesting player is the one who paused it.
     * 鑻ヨ姹傛柟鏄殏鍋滄父鎴忕殑鐜╁锛屽垯鎭㈠娓告垙銆?
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
     * 浠庡唴瀛樹腑鐨?Game 瀵硅薄鏋勫缓蹇収 JSONObject锛屼娇鐢ㄤ笌 start-matching 涓€鑷寸殑 snake_case 瀛楁鍚嶃€?
     * 杩欐牱鍗充娇 Redis 涓嶅彲鐢紝涔熻兘姝ｇ‘涓嬪彂蹇収缁欏墠绔€?
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

    /** 鍚戝綋鍓嶇敤鎴蜂笅鍙?game-resync锛堜紭鍏堜粠鍐呭瓨 Game 鏋勫缓锛屽厹搴曟煡 Redis锛?*/
    private GameSnapshot loadSnapshotForCurrentUser() {
        if (user == null || gameSnapshotService == null) return null;
        try {
            return gameSnapshotService.getByUserId(user.getId());
        } catch (Exception e) {
            System.err.println("[Redis] loadSnapshotForCurrentUser failed: " + e.getMessage());
            return null;
        }
    }
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

    /** 鐜╁杩斿洖 PK 椤甸潰锛氫粠 away 鍒楄〃绉婚櫎锛岃嫢鍙屾柟閮藉湪鍒欒В闄ゆ寕璧?*/
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

    /** 鐜╁绂诲紑 PK 椤甸潰鎴栨柇绾匡細鏍囪鏆傜锛屾寕璧峰灞€ */
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

    private void gameLeave(String reason) {
        if (game == null) return;
        markPlayerLeave(game, user.getId(), reason);
    }

    /** onOpen 鏃惰嚜鍔ㄦ煡 Redis锛屽鏈夎繘琛屼腑瀵瑰眬鍒欎笅鍙?game-resync */
    private void autoSyncGame(Integer userId) {
        syncGame();
    }

    /**
     * Handles incoming WebSocket messages and dispatches to the appropriate handler by event type.
     * 澶勭悊浼犲叆鐨刉ebSocket娑堟伅锛屽苟鎸変簨浠剁被鍨嬪垎鍙戝埌瀵瑰簲澶勭悊鍣ㄣ€?
     */
    @OnMessage
    /**
     * Handles onMessage.
     * ??onMessage?
     */
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
     * Handles WebSocket errors by printing the stack trace.
     * 閫氳繃鎵撳嵃鍫嗘爤璺熻釜澶勭悊WebSocket閿欒銆?
     */
    @OnError
    /**
     * Handles onError.
     * ??onError?
     */
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /**
     * Sends a text message to the connected client synchronously.
     * 鍚屾鍚戝凡杩炴帴瀹㈡埛绔彂閫佹枃鏈秷鎭€?
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



