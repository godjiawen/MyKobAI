package com.kob.backend.controller.friends;

import com.kob.backend.service.friends.FriendChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FriendChatController {
    @Autowired
    private FriendChatService friendChatService;

    @GetMapping("/api/friends/chat/conversations/")
    /**
     * Handles conversations.
     * ??conversations?
     */
    public Map<String, Object> conversations() {
        return friendChatService.conversations();
    }

    @GetMapping("/api/friends/chat/history/")
    public Map<String, Object> history(
            @RequestParam("friend_id") Integer friendId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "50") Integer pageSize) {
        return friendChatService.history(friendId, page, pageSize);
    }

    @PostMapping("/api/friends/chat/send/")
    public Map<String, Object> send(
            @RequestParam("friend_id") Integer friendId,
            @RequestParam("content") String content) {
        return friendChatService.send(friendId, content);
    }

    @PostMapping("/api/friends/chat/read/")
    /**
     * Handles read.
     * ??read?
     */
    public Map<String, Object> read(@RequestParam("friend_id") Integer friendId) {
        return friendChatService.read(friendId);
    }
}
