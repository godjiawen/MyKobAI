package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
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

@Component
@ServerEndpoint("/websocket/{token}")
public class WebSocketServer {

    public static final ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>();

    private User user;
    private Session session = null;

    public static UserMapper userMapper;
    public static RecordMapper recordMapper;
    private static BotMapper botMapper;
    public static RestTemplate restTemplate;
    public Game game = null;
    private static final String addPlayerUrl = "http://127.0.0.1:3001/player/add/";
    private static final String removePlayerUrl = "http://127.0.0.1:3001/player/remove/";

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    @Autowired
    public void setRecordMapper(RecordMapper recordMapper) {
        WebSocketServer.recordMapper = recordMapper;
    }

    @Autowired
    public void setBotMapper(BotMapper botMapper) {
        WebSocketServer.botMapper = botMapper;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        WebSocketServer.restTemplate = restTemplate;
    }

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
        } else {
            this.session.close();
        }
        System.out.println(users);
    }

    @OnClose
    public void onClose() {
        System.out.println("disconnected!");
        if (this.user != null) {
            this.game = null;
            users.remove(this.user.getId());
        }
    }

    public static void startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId) {
        User a = userMapper.selectById(aId);
        User b = userMapper.selectById(bId);
        Bot botA = botMapper.selectById(aBotId);
        Bot botB = botMapper.selectById(bBotId);

        Game game = new Game(13, 14, 20, a.getId(), botA, b.getId(), botB);
        game.createMap();
        if (users.get(a.getId()) != null) users.get(a.getId()).game = game;
        if (users.get(b.getId()) != null) users.get(b.getId()).game = game;

        game.start();

        JSONObject respGame = new JSONObject();
        respGame.put("a_id", game.getPlayerA().getId());
        respGame.put("b_id", game.getPlayerB().getId());
        respGame.put("a_sx", game.getPlayerA().getSx());
        respGame.put("b_sx", game.getPlayerB().getSx());
        respGame.put("a_sy", game.getPlayerA().getSy());
        respGame.put("b_sy", game.getPlayerB().getSy());
        respGame.put("map", game.getG());

        String roomId = Math.min(aId, bId) + "_" + Math.max(aId, bId);

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

    public static boolean isUserOnline(Integer userId) {
        return userId != null && users.containsKey(userId);
    }

    public static boolean isUserInGame(Integer userId) {
        if (userId == null) return false;
        WebSocketServer server = users.get(userId);
        return server != null && server.game != null;
    }

    public static String getUserOnlineStatus(Integer userId) {
        if (!isUserOnline(userId)) return "offline";
        if (isUserInGame(userId)) return "in_game";
        return "online";
    }

    public static void clearGame(Integer userId) {
        WebSocketServer server = users.get(userId);
        if (server != null) {
            server.game = null;
        }
    }

    public static void clearGame(Integer leftUserId, Integer rightUserId) {
        clearGame(leftUserId);
        clearGame(rightUserId);
    }

    public static void sendEvent(Integer userId, JSONObject event) {
        if (userId == null || event == null) return;
        WebSocketServer server = users.get(userId);
        if (server != null) {
            server.sendMessage(event.toJSONString());
        }
    }

    private void startMatching(Integer botId) {
        System.out.println("start matching!");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        data.add("rating", this.user.getRating().toString());
        data.add("bot_id", botId.toString());
        restTemplate.postForObject(addPlayerUrl, data, String.class);
    }

    private void stopMatching() {
        System.out.println("stop matching");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    private void move(int direction) {
        if (game == null) return;
        if (game.isPaused()) return;
        if (game.getPlayerA().getId().equals(user.getId())) {
            if (game.getPlayerA().getBotId().equals(-1)) game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(user.getId())) {
            if (game.getPlayerB().getBotId().equals(-1)) game.setNextStepB(direction);
        }
    }

    private void broadcastToGame(String message) {
        Integer aId = game.getPlayerA().getId();
        Integer bId = game.getPlayerB().getId();
        if (users.get(aId) != null) users.get(aId).sendMessage(message);
        if (users.get(bId) != null) users.get(bId).sendMessage(message);
    }

    private void pause() {
        if (game == null) return;
        if (game.isPaused()) return;

        String by = game.getPlayerA().getId().equals(user.getId()) ? "A" : "B";
        game.setPaused(true, by);

        JSONObject resp = new JSONObject();
        resp.put("event", "game-paused");
        resp.put("paused_by", user.getId());
        broadcastToGame(resp.toJSONString());
    }

    private void resume() {
        if (game == null) return;
        if (!game.isPaused()) return;

        String by = game.getPlayerA().getId().equals(user.getId()) ? "A" : "B";
        if (!by.equals(game.getPausedBy())) return;

        game.setPaused(false, "");

        JSONObject resp = new JSONObject();
        resp.put("event", "game-resumed");
        broadcastToGame(resp.toJSONString());
    }

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
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

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
