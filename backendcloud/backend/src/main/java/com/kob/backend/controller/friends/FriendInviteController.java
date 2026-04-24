package com.kob.backend.controller.friends;

import com.kob.backend.service.friends.FriendInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FriendInviteController {
    @Autowired
    private FriendInviteService friendInviteService;

    @PostMapping("/api/friends/invite/send/")
    public Map<String, Object> send(
            @RequestParam("receiver_id") Integer receiverId,
            @RequestParam(value = "sender_bot_id", defaultValue = "-1") Integer senderBotId,
            @RequestParam(value = "game_mode", defaultValue = "pk") String gameMode,
            @RequestParam(value = "map_id", required = false) Integer mapId,
            @RequestParam(value = "room_name", defaultValue = "") String roomName,
            @RequestParam(value = "round_seconds", defaultValue = "15") Integer roundSeconds,
            @RequestParam(value = "allow_spectator", defaultValue = "true") Boolean allowSpectator) {
        return friendInviteService.send(receiverId, senderBotId, gameMode, mapId, roomName, roundSeconds, allowSpectator);
    }

    @PostMapping("/api/friends/invite/respond/")
    public Map<String, Object> respond(
            @RequestParam("invite_id") Integer inviteId,
            @RequestParam("action") String action,
            @RequestParam(value = "receiver_bot_id", required = false) Integer receiverBotId) {
        return friendInviteService.respond(inviteId, action, receiverBotId);
    }

    /**
     * 处理 pending 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of pending with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @GetMapping("/api/friends/invite/pending/")
    public Map<String, Object> pending() {
        return friendInviteService.pending();
    }
}
