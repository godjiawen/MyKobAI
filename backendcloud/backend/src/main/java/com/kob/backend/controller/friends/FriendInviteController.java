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
            @RequestParam(value = "game_mode", defaultValue = "pk") String gameMode) {
        return friendInviteService.send(receiverId, senderBotId, gameMode);
    }

    @PostMapping("/api/friends/invite/respond/")
    public Map<String, Object> respond(
            @RequestParam("invite_id") Integer inviteId,
            @RequestParam("action") String action,
            @RequestParam(value = "receiver_bot_id", required = false) Integer receiverBotId) {
        return friendInviteService.respond(inviteId, action, receiverBotId);
    }

    @GetMapping("/api/friends/invite/pending/")
    /**
     * Handles pending.
     * ??pending?
     */
    public Map<String, Object> pending() {
        return friendInviteService.pending();
    }
}
