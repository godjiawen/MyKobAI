package com.kob.chatsystem.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.chatsystem.utils.JwtUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 对局聊天 WebSocket 端点。
 * 地址：/chat/{token}/{roomId}
 *
 * roomId 约定为 "{min(aId, bId)}_{max(aId, bId)}"，由后端在开局时下发。
 * 鉴权方式：路径中携带令牌，服务端解析 userId，用户名由客户端消息提供。
 *
 * 入站消息：{ "content": "...", "username": "..." }
 * 出站消息：{ "userId": 1, "username": "...", "content": "...", "timestamp": 1234567890 }
 */
@Component
@ServerEndpoint("/chat/{token}/{roomId}")
public class ChatWebSocketServer {

    /** roomId 到该房间连接集合的映射 */
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<ChatWebSocketServer>> rooms =
            new ConcurrentHashMap<>();

    private Session session;
    private String  roomId;
    private Integer userId;

    // 连接生命周期

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("token")  String token,
                       @PathParam("roomId") String roomId) throws IOException {
        this.session = session;

        Integer uid;
        try {
            uid = JwtUtil.getUserId(token);
        } catch (Exception e) {
            System.err.println("[Chat] Rejected invalid token, closing session");
            session.close();
            return;
        }

        this.userId = uid;
        this.roomId = roomId;

        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(this);
        System.out.println("[Chat] Connected: userId=" + userId + " room=" + roomId
                + " | room size=" + rooms.get(roomId).size());
    }

    @OnClose
    public void onClose() {
        if (roomId != null) {
            CopyOnWriteArraySet<ChatWebSocketServer> room = rooms.get(roomId);
            if (room != null) {
                room.remove(this);
                if (room.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
        System.out.println("[Chat] Disconnected: userId=" + userId + " room=" + roomId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    // 消息收发

    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject data;
        try {
            data = JSONObject.parseObject(message);
        } catch (Exception e) {
            System.err.println("[Chat] Malformed message from userId=" + userId);
            return;
        }

        String content  = data.getString("content");
        String username = data.getString("username");

        if (content == null || content.isBlank()) return;

        JSONObject broadcast = new JSONObject();
        broadcast.put("userId",    this.userId);
        broadcast.put("username",  username != null ? username.trim() : "Player");
        broadcast.put("content",   content.trim());
        broadcast.put("timestamp", System.currentTimeMillis());

        String msgStr = broadcast.toJSONString();

        CopyOnWriteArraySet<ChatWebSocketServer> room = rooms.get(roomId);
        if (room != null) {
            for (ChatWebSocketServer server : room) {
                server.sendMessage(msgStr);
            }
        }
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

