package com.kob.backend.service.friends;

import java.util.Map;

public interface FriendInviteService {
    Map<String, Object> send(Integer receiverId, Integer senderBotId, String gameMode);

    Map<String, Object> respond(Integer inviteId, String action, Integer receiverBotId);

    Map<String, Object> pending();
}
