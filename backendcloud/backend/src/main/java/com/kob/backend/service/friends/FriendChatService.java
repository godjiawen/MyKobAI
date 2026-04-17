package com.kob.backend.service.friends;

import java.util.Map;

public interface FriendChatService {
    Map<String, Object> conversations();

    Map<String, Object> history(Integer friendId, Integer page, Integer pageSize);

    Map<String, Object> send(Integer friendId, String content);

    Map<String, Object> read(Integer friendId);
}
