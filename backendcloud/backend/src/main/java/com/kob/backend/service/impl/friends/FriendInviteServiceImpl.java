package com.kob.backend.service.impl.friends;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.mapper.FriendInviteMapper;
import com.kob.backend.pojo.FriendInvite;
import com.kob.backend.pojo.User;
import com.kob.backend.service.friends.FriendInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendInviteServiceImpl implements FriendInviteService {
    private static final long INVITE_EXPIRE_MS = 30_000L;

    @Autowired
    private FriendInviteMapper friendInviteMapper;
    @Autowired
    private FriendDomainService friendDomainService;

    @Override
    @Transactional
    /**
     * Handles send.
     * ??send?
     */
    public Map<String, Object> send(Integer receiverId, Integer senderBotId, String gameMode) {
        User currentUser = friendDomainService.currentUser();
        if (receiverId == null) return error("receiver_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), receiverId)) return error("not your friend");
        if (currentUser.getId().equals(receiverId)) return error("cannot invite yourself");

        String receiverStatus = friendDomainService.getOnlineStatus(receiverId);
        if ("offline".equals(receiverStatus)) return error("friend is offline");
        if ("in_game".equals(receiverStatus)) return error("friend is in game");
        if (friendDomainService.getPendingInviteBetween(currentUser.getId(), receiverId) != null) return error("invite already exists");

        Date now = new Date();
        FriendInvite invite = new FriendInvite(
                null,
                currentUser.getId(),
                receiverId,
                gameMode == null || gameMode.isBlank() ? "pk" : gameMode,
                senderBotId == null ? -1 : senderBotId,
                null,
                "pending",
                now,
                new Date(now.getTime() + INVITE_EXPIRE_MS),
                null
        );
        friendInviteMapper.insert(invite);
        friendDomainService.notifyInviteReceived(invite);

        Map<String, Object> resp = success();
        Map<String, Object> inviteData = new LinkedHashMap<>();
        inviteData.put("invite_id", invite.getId());
        inviteData.put("status", invite.getStatus());
        inviteData.put("expired_at", com.kob.backend.utils.DateTimeUtil.format(invite.getExpiredAt()));
        resp.put("invite", inviteData);
        return resp;
    }

    @Override
    @Transactional
    /**
     * Handles respond.
     * ??respond?
     */
    public Map<String, Object> respond(Integer inviteId, String action, Integer receiverBotId) {
        User currentUser = friendDomainService.currentUser();
        if (inviteId == null) return error("invite_id is required");
        if (action == null || action.isBlank()) return error("action is required");

        FriendInvite invite = friendInviteMapper.selectById(inviteId);
        if (invite == null) return error("invite not found");
        friendDomainService.expireInviteIfNeeded(invite);
        if (!"pending".equals(invite.getStatus())) return error("invite already handled");

        String normalizedAction = action.trim().toLowerCase();
        if ("accept".equals(normalizedAction)) {
            if (!currentUser.getId().equals(invite.getReceiverId())) return error("no permission");
            invite.setStatus("accepted");
            invite.setReceiverBotId(receiverBotId == null ? -1 : receiverBotId);
            invite.setHandledAt(new Date());
            friendInviteMapper.updateById(invite);
            friendDomainService.notifyInviteUpdated(invite, invite.getSenderId());
            friendDomainService.notifyInviteUpdated(invite, invite.getReceiverId());
            WebSocketServer.startGame(invite.getSenderId(), invite.getSenderBotId(), invite.getReceiverId(), invite.getReceiverBotId());

            Map<String, Object> resp = success();
            Map<String, Object> inviteData = new LinkedHashMap<>();
            inviteData.put("invite_id", invite.getId());
            inviteData.put("status", invite.getStatus());
            resp.put("invite", inviteData);

            Map<String, Object> match = new LinkedHashMap<>();
            match.put("a_id", invite.getSenderId());
            match.put("a_bot_id", invite.getSenderBotId());
            match.put("b_id", invite.getReceiverId());
            match.put("b_bot_id", invite.getReceiverBotId());
            resp.put("match", match);
            return resp;
        }

        if ("reject".equals(normalizedAction)) {
            if (!currentUser.getId().equals(invite.getReceiverId())) return error("no permission");
            invite.setStatus("rejected");
            invite.setHandledAt(new Date());
            friendInviteMapper.updateById(invite);
            friendDomainService.notifyInviteUpdated(invite, invite.getSenderId());
            friendDomainService.notifyInviteUpdated(invite, invite.getReceiverId());
            return buildInviteOnlyResponse(invite);
        }

        if ("cancel".equals(normalizedAction)) {
            if (!currentUser.getId().equals(invite.getSenderId())) return error("no permission");
            invite.setStatus("cancelled");
            invite.setHandledAt(new Date());
            friendInviteMapper.updateById(invite);
            friendDomainService.notifyInviteUpdated(invite, invite.getSenderId());
            friendDomainService.notifyInviteUpdated(invite, invite.getReceiverId());
            return buildInviteOnlyResponse(invite);
        }

        return error("invalid action");
    }

    @Override
    /**
     * Handles pending.
     * ??pending?
     */
    public Map<String, Object> pending() {
        User currentUser = friendDomainService.currentUser();
        friendDomainService.expirePendingInvitesForUser(currentUser.getId());
        QueryWrapper<FriendInvite> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "pending")
                .and(query -> query.eq("sender_id", currentUser.getId()).or().eq("receiver_id", currentUser.getId()))
                .orderByDesc("id");
        List<FriendInvite> invites = friendInviteMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (FriendInvite invite : invites) {
            items.add(friendDomainService.buildInviteView(invite));
        }
        Map<String, Object> resp = success();
        resp.put("invites", items);
        return resp;
    }

    /**
     * Handles buildInviteOnlyResponse.
     * ??buildInviteOnlyResponse?
     */
    private Map<String, Object> buildInviteOnlyResponse(FriendInvite invite) {
        Map<String, Object> resp = success();
        Map<String, Object> inviteData = new LinkedHashMap<>();
        inviteData.put("invite_id", invite.getId());
        inviteData.put("status", invite.getStatus());
        resp.put("invite", inviteData);
        return resp;
    }

    /**
     * Handles success.
     * ??success?
     */
    private Map<String, Object> success() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        return resp;
    }

    /**
     * Handles error.
     * ??error?
     */
    private Map<String, Object> error(String errorMessage) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", errorMessage);
        return resp;
    }
}
