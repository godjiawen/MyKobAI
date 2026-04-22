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

    /**
     * 处理 conversations 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of conversations with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @GetMapping("/api/friends/chat/conversations/")
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

    /**
     * 处理 RequestParam 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of RequestParam with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @param content 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/friends/chat/send/")
    public Map<String, Object> send(
            @RequestParam("friend_id") Integer friendId,
            @RequestParam("content") String content) {
        return friendChatService.send(friendId, content);
    }

    /**
     * 处理 RequestParam 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of RequestParam with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @PostMapping("/api/friends/chat/read/")
    public Map<String, Object> read(@RequestParam("friend_id") Integer friendId) {
        return friendChatService.read(friendId);
    }
}