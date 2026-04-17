package com.kob.backend.service.friends;

import java.util.Map;

public interface FriendRequestService {
    Map<String, Object> send(Integer receiverId, String message);

    Map<String, Object> received(String status, Integer page, Integer pageSize);

    Map<String, Object> sent(String status, Integer page, Integer pageSize);

    Map<String, Object> accept(Integer requestId);

    Map<String, Object> ignore(Integer requestId);

    Map<String, Object> cancel(Integer requestId);
}
