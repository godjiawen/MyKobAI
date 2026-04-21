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

/**
 * Domain service providing shared business logic and view building for the friend subsystem.
 * 为好友子系统提供共享业务逻辑和视图构建的领域服务。
 */
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

    /**
     * Returns the currently authenticated user from the security context.
     * 从安全上下文中返回当前已认证用户。
     */
    public User currentUser() {
        return AuthUtil.getCurrentUser();
    }

    /**
     * Retrieves the friend relation record from userId to friendId, or null if not friends.
     * 查询userId到friendId的好友关系记录，不存在则返回null。
     */
    public FriendRelation getRelation(Integer userId, Integer friendId) {
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("friend_id", friendId);
        return friendRelationMapper.selectOne(wrapper);
    }

    /**
     * Returns true if a bidirectional friend relation exists between the two users.
     * 若两用户之间存在好友关系则返回true。
     */
    public boolean areFriends(Integer userId, Integer friendId) {
        return getRelation(userId, friendId) != null;
    }

    /**
     * Returns the pending friend request sent from senderId to receiverId, or null.
     * 返回senderId向receiverId发送的待处理好友请求，不存在则返回null。
     */
    public FriendRequest getPendingRequest(Integer senderId, Integer receiverId) {
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", senderId)
                .eq("receiver_id", receiverId)
                .eq("status", "pending")
                .orderByDesc("id")
                .last("limit 1");
        return friendRequestMapper.selectOne(wrapper);
    }

    /**
     * Returns the ID of any pending request between the two users in either direction, or null.
     * 返回两用户之间任意方向待处理请求的ID，不存在则返回null。
     */
    public Integer getPendingRequestId(Integer currentUserId, Integer targetUserId) {
        FriendRequest sent = getPendingRequest(currentUserId, targetUserId);
        if (sent != null) return sent.getId();
        FriendRequest received = getPendingRequest(targetUserId, currentUserId);
        return received != null ? received.getId() : null;
    }

    /**
     * Returns the relation status string between currentUser and targetUser: self/friend/sent_pending/received_pending/none.
     * 返回当前用户与目标用户之间的关系状态字符串：self/friend/sent_pending/received_pending/none。
     */
    public String getRelationStatus(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) return "self";
        if (areFriends(currentUserId, targetUserId)) return "friend";
        if (getPendingRequest(currentUserId, targetUserId) != null) return "sent_pending";
        if (getPendingRequest(targetUserId, currentUserId) != null) return "received_pending";
        return "none";
    }

    /**
     * Returns the latest pending game invite between two users, expiring it first if needed.
     * 返回两用户之间最新的待处理游戏邀请，必要时先令其过期。
     */
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

    /**
     * Marks the given invite as expired if its expiry time has passed.
     * 若邀请的过期时间已过，则将其标记为已过期。
     */
    public void expireInviteIfNeeded(FriendInvite invite) {
        if (invite == null || invite.getExpiredAt() == null || !"pending".equals(invite.getStatus())) return;
        if (!invite.getExpiredAt().before(new Date())) return;
        invite.setStatus("expired");
        invite.setHandledAt(new Date());
        friendInviteMapper.updateById(invite);
    }

    /**
     * Expires all pending invites for a user that have passed their expiry time.
     * 将指定用户所有已超期的待处理邀请标记为已过期。
     */
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

    /**
     * Returns the online status of a user by querying the WebSocket server.
     * 通过查询WebSocket服务器返回用户的在线状态。
     */
    public String getOnlineStatus(Integer userId) {
        return WebSocketServer.getUserOnlineStatus(userId);
    }

    /**
     * Builds a map view of a friend relation for API responses including stats and status.
     * 构建好友关系的Map视图（含统计和状态）用于API响应。
     */
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

    /**
     * Builds a map view of a friend request for API responses.
     * 构建好友请求的Map视图用于API响应。
     */
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

    /**
     * Builds a map view of a game invite for API responses.
     * 构建游戏邀请的Map视图用于API响应。
     */
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

    /**
     * Sends a WebSocket event to the receiver notifying them of a new friend request.
     * 向接收方发送WebSocket事件，通知其收到新的好友请求。
     */
    public void notifyFriendRequestReceived(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-received");
        event.put("request", buildRequestView(request));
        WebSocketServer.sendEvent(request.getReceiverId(), event);
    }

    /**
     * Sends a WebSocket event to the sender notifying them that their request has been handled.
     * 向发送方发送WebSocket事件，通知其好友请求已被处理。
     */
    public void notifyFriendRequestHandled(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-handled");
        event.put("request_id", request.getId());
        event.put("status", request.getStatus());
        WebSocketServer.sendEvent(request.getSenderId(), event);
    }

    /**
     * Sends a WebSocket event to the receiver notifying them of a new game invite.
     * 向接收方发送WebSocket事件，通知其收到新的游戏邀请。
     */
    public void notifyInviteReceived(FriendInvite invite) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-invite-received");
        event.put("invite", buildInviteView(invite));
        WebSocketServer.sendEvent(invite.getReceiverId(), event);
    }

    /**
     * Sends a WebSocket event to the target user notifying them that a game invite was updated.
     * 向目标用户发送WebSocket事件，通知其游戏邀请状态已更新。
     */
    public void notifyInviteUpdated(FriendInvite invite, Integer targetUserId) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-invite-updated");
        event.put("invite_id", invite.getId());
        event.put("status", invite.getStatus());
        WebSocketServer.sendEvent(targetUserId, event);
    }

    /**
     * Resolves the last active time of a friend: now if online, latest match time, or relation creation date.
     * 解析好友的最后活跃时间：在线返回当前时间，否则为最近对局时间或建立好友关系时间。
     */
    private Date resolveLastActiveAt(FriendRelation relation, Record latestRecord) {
        String status = getOnlineStatus(relation.getFriendId());
        if (!"offline".equals(status)) return new Date();
        if (latestRecord != null && latestRecord.getCreatetime() != null) return latestRecord.getCreatetime();
        return relation.getCreatedAt();
    }

    /**
     * Returns the total number of matches played between two users.
     * 返回两用户之间总对局次数。
     */
    private Long getMatchCount(Integer currentUserId, Integer friendId) {
        return recordMapper.selectCount(buildPairRecordWrapper(currentUserId, friendId));
    }

    /**
     * Returns the most recent game record between two users, or null if none exists.
     * 返回两用户之间最近一场对局记录，不存在则返回null。
     */
    private Record getLatestRecord(Integer currentUserId, Integer friendId) {
        QueryWrapper<Record> wrapper = buildPairRecordWrapper(currentUserId, friendId);
        wrapper.orderByDesc("createtime").last("limit 1");
        return recordMapper.selectOne(wrapper);
    }

    /**
     * Builds a query wrapper matching records where either user was player A or B.
     * 构建查询包装器，匹配任意一方担任玩家A或B的对局记录。
     */
    private QueryWrapper<Record> buildPairRecordWrapper(Integer leftUserId, Integer rightUserId) {
        QueryWrapper<Record> wrapper = new QueryWrapper<>();
        wrapper.and(query -> query
                .and(item -> item.eq("a_id", leftUserId).eq("b_id", rightUserId))
                .or(item -> item.eq("a_id", rightUserId).eq("b_id", leftUserId)));
        return wrapper;
    }
}
