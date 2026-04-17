package com.kob.backend.service.impl.friends;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.FriendInviteMapper;
import com.kob.backend.mapper.FriendRelationMapper;
import com.kob.backend.mapper.FriendRequestMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.FriendInvite;
import com.kob.backend.pojo.FriendRelation;
import com.kob.backend.pojo.FriendRequest;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import com.kob.backend.utils.AuthUtil;
import com.kob.backend.utils.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FriendDomainService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BotMapper botMapper;
    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private FriendInviteMapper friendInviteMapper;

    public User currentUser() {
        return AuthUtil.getCurrentUser();
    }

    public FriendRelation getRelation(Integer userId, Integer friendId) {
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("friend_id", friendId);
        return friendRelationMapper.selectOne(wrapper);
    }

    public boolean areFriends(Integer userId, Integer friendId) {
        return getRelation(userId, friendId) != null;
    }

    public FriendRequest getPendingRequest(Integer senderId, Integer receiverId) {
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", senderId)
                .eq("receiver_id", receiverId)
                .eq("status", "pending")
                .orderByDesc("id")
                .last("limit 1");
        return friendRequestMapper.selectOne(wrapper);
    }

    public Integer getPendingRequestId(Integer currentUserId, Integer targetUserId) {
        FriendRequest sent = getPendingRequest(currentUserId, targetUserId);
        if (sent != null) return sent.getId();
        FriendRequest received = getPendingRequest(targetUserId, currentUserId);
        return received != null ? received.getId() : null;
    }

    public String getRelationStatus(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) return "self";
        if (areFriends(currentUserId, targetUserId)) return "friend";
        if (getPendingRequest(currentUserId, targetUserId) != null) return "sent_pending";
        if (getPendingRequest(targetUserId, currentUserId) != null) return "received_pending";
        return "none";
    }

    public FriendInvite getPendingInviteBetween(Integer leftUserId, Integer rightUserId) {
        QueryWrapper<FriendInvite> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "pending")
                .and(query -> query
                        .and(item -> item.eq("sender_id", leftUserId).eq("receiver_id", rightUserId))
                        .or(item -> item.eq("sender_id", rightUserId).eq("receiver_id", leftUserId)))
                .orderByDesc("id")
                .last("limit 1");
        FriendInvite invite = friendInviteMapper.selectOne(wrapper);
        if (invite != null) {
            expireInviteIfNeeded(invite);
            if (!"pending".equals(invite.getStatus())) {
                return null;
            }
        }
        return invite;
    }

    public void expireInviteIfNeeded(FriendInvite invite) {
        if (invite == null || invite.getExpiredAt() == null || !"pending".equals(invite.getStatus())) return;
        if (!invite.getExpiredAt().before(new Date())) return;
        invite.setStatus("expired");
        invite.setHandledAt(new Date());
        friendInviteMapper.updateById(invite);
    }

    public void expirePendingInvitesForUser(Integer userId) {
        QueryWrapper<FriendInvite> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "pending")
                .lt("expired_at", new Date())
                .and(query -> query.eq("sender_id", userId).or().eq("receiver_id", userId));
        List<FriendInvite> invites = friendInviteMapper.selectList(wrapper);
        for (FriendInvite invite : invites) {
            invite.setStatus("expired");
            invite.setHandledAt(new Date());
            friendInviteMapper.updateById(invite);
        }
    }

    public String getOnlineStatus(Integer userId) {
        return WebSocketServer.getUserOnlineStatus(userId);
    }

    public Map<String, Object> buildFriendView(Integer currentUserId, FriendRelation relation) {
        User friend = userMapper.selectById(relation.getFriendId());
        Map<String, Object> item = new LinkedHashMap<>();
        if (friend == null) {
            item.put("friend_id", relation.getFriendId());
            item.put("friend_username", "(已注销)");
            item.put("friend_photo", "");
            item.put("friend_rating", 0);
        } else {
            item.put("friend_id", friend.getId());
            item.put("friend_username", friend.getUsername());
            item.put("friend_photo", friend.getPhoto());
            item.put("friend_rating", friend.getRating());
        }
        Record latestRecord = getLatestRecord(currentUserId, relation.getFriendId());
        item.put("online_status", getOnlineStatus(relation.getFriendId()));
        item.put("last_active_at", DateTimeUtil.format(resolveLastActiveAt(relation, latestRecord)));
        item.put("last_match_at", DateTimeUtil.format(latestRecord != null ? latestRecord.getCreatetime() : null));
        item.put("match_count", getMatchCount(currentUserId, relation.getFriendId()));
        item.put("is_favorite", Boolean.TRUE.equals(relation.getIsFavorite()));
        item.put("created_at", DateTimeUtil.format(relation.getCreatedAt()));
        return item;
    }

    public Map<String, Object> buildRequestView(FriendRequest request) {
        User sender = userMapper.selectById(request.getSenderId());
        User receiver = userMapper.selectById(request.getReceiverId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("request_id", request.getId());
        item.put("sender_id", request.getSenderId());
        item.put("sender_username", sender != null ? sender.getUsername() : "(已注销)");
        item.put("sender_photo", sender != null ? sender.getPhoto() : "");
        item.put("sender_rating", sender != null ? sender.getRating() : 0);
        item.put("receiver_id", request.getReceiverId());
        item.put("receiver_username", receiver != null ? receiver.getUsername() : "(已注销)");
        item.put("receiver_photo", receiver != null ? receiver.getPhoto() : "");
        item.put("receiver_rating", receiver != null ? receiver.getRating() : 0);
        item.put("message", request.getMessage());
        item.put("status", request.getStatus());
        item.put("created_at", DateTimeUtil.format(request.getCreatedAt()));
        item.put("handled_at", DateTimeUtil.format(request.getHandledAt()));
        return item;
    }

    public Map<String, Object> buildInviteView(FriendInvite invite) {
        User sender = userMapper.selectById(invite.getSenderId());
        User receiver = userMapper.selectById(invite.getReceiverId());
        Bot senderBot = invite.getSenderBotId() != null && invite.getSenderBotId() != -1
                ? botMapper.selectById(invite.getSenderBotId())
                : null;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("invite_id", invite.getId());
        item.put("sender_id", invite.getSenderId());
        item.put("sender_username", sender != null ? sender.getUsername() : "(已注销)");
        item.put("sender_photo", sender != null ? sender.getPhoto() : "");
        item.put("receiver_id", invite.getReceiverId());
        item.put("receiver_username", receiver != null ? receiver.getUsername() : "(已注销)");
        item.put("receiver_photo", receiver != null ? receiver.getPhoto() : "");
        item.put("game_mode", invite.getGameMode());
        item.put("sender_bot_id", invite.getSenderBotId());
        item.put("sender_bot_title", senderBot != null ? senderBot.getTitle() : "manual");
        item.put("status", invite.getStatus());
        item.put("created_at", DateTimeUtil.format(invite.getCreatedAt()));
        item.put("expired_at", DateTimeUtil.format(invite.getExpiredAt()));
        return item;
    }

    public void notifyFriendRequestReceived(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-received");
        event.put("request", buildRequestView(request));
        WebSocketServer.sendEvent(request.getReceiverId(), event);
    }

    public void notifyFriendRequestHandled(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-handled");
        event.put("request_id", request.getId());
        event.put("status", request.getStatus());
        WebSocketServer.sendEvent(request.getSenderId(), event);
    }

    public void notifyInviteReceived(FriendInvite invite) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-invite-received");
        event.put("invite", buildInviteView(invite));
        WebSocketServer.sendEvent(invite.getReceiverId(), event);
    }

    public void notifyInviteUpdated(FriendInvite invite, Integer targetUserId) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-invite-updated");
        event.put("invite_id", invite.getId());
        event.put("status", invite.getStatus());
        WebSocketServer.sendEvent(targetUserId, event);
    }

    private Date resolveLastActiveAt(FriendRelation relation, Record latestRecord) {
        String status = getOnlineStatus(relation.getFriendId());
        if (!"offline".equals(status)) return new Date();
        if (latestRecord != null && latestRecord.getCreatetime() != null) return latestRecord.getCreatetime();
        return relation.getCreatedAt();
    }

    private Long getMatchCount(Integer currentUserId, Integer friendId) {
        return recordMapper.selectCount(buildPairRecordWrapper(currentUserId, friendId));
    }

    private Record getLatestRecord(Integer currentUserId, Integer friendId) {
        QueryWrapper<Record> wrapper = buildPairRecordWrapper(currentUserId, friendId);
        wrapper.orderByDesc("createtime").last("limit 1");
        return recordMapper.selectOne(wrapper);
    }

    private QueryWrapper<Record> buildPairRecordWrapper(Integer leftUserId, Integer rightUserId) {
        QueryWrapper<Record> wrapper = new QueryWrapper<>();
        wrapper.and(query -> query
                .and(item -> item.eq("a_id", leftUserId).eq("b_id", rightUserId))
                .or(item -> item.eq("a_id", rightUserId).eq("b_id", leftUserId)));
        return wrapper;
    }
}
