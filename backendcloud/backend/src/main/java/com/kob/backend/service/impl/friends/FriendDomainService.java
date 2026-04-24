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
     * 处理 currentUser 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of currentUser with controlled input and output handling.
     *
     * @return 返回 User 类型结果；Returns a result of type User.
     */
    public User currentUser() {
        return AuthUtil.getCurrentUser();
    }

    /**
     * 查询并返回 getRelation 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRelation with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param friendId 标识参数；Identifier value.
     * @return 返回 FriendRelation 类型结果；Returns a result of type FriendRelation.
     */
    public FriendRelation getRelation(Integer userId, Integer friendId) {
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("friend_id", friendId);
        return friendRelationMapper.selectOne(wrapper);
    }

    /**
     * 处理 areFriends 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of areFriends with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param friendId 标识参数；Identifier value.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    public boolean areFriends(Integer userId, Integer friendId) {
        return getRelation(userId, friendId) != null;
    }

    /**
     * 查询并返回 getPendingRequest 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPendingRequest with controlled input and output handling.
     *
     * @param senderId 标识参数；Identifier value.
     * @param receiverId 标识参数；Identifier value.
     * @return 返回 FriendRequest 类型结果；Returns a result of type FriendRequest.
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
     * 查询并返回 getPendingRequestId 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPendingRequestId with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param targetUserId 标识参数；Identifier value.
     * @return 返回数值结果；Returns a numeric result.
     */
    public Integer getPendingRequestId(Integer currentUserId, Integer targetUserId) {
        FriendRequest sent = getPendingRequest(currentUserId, targetUserId);
        if (sent != null) return sent.getId();
        FriendRequest received = getPendingRequest(targetUserId, currentUserId);
        return received != null ? received.getId() : null;
    }

    /**
     * 查询并返回 getRelationStatus 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRelationStatus with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param targetUserId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    public String getRelationStatus(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) return "self";
        if (areFriends(currentUserId, targetUserId)) return "friend";
        if (getPendingRequest(currentUserId, targetUserId) != null) return "sent_pending";
        if (getPendingRequest(targetUserId, currentUserId) != null) return "received_pending";
        return "none";
    }

    /**
     * 查询并返回 getPendingInviteBetween 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPendingInviteBetween with controlled input and output handling.
     *
     * @param leftUserId 标识参数；Identifier value.
     * @param rightUserId 标识参数；Identifier value.
     * @return 返回 FriendInvite 类型结果；Returns a result of type FriendInvite.
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
     * 处理 expireInviteIfNeeded 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of expireInviteIfNeeded with controlled input and output handling.
     *
     * @param invite 输入参数；Input parameter.
     */
    public void expireInviteIfNeeded(FriendInvite invite) {
        if (invite == null || invite.getExpiredAt() == null || !"pending".equals(invite.getStatus())) return;
        if (!invite.getExpiredAt().before(new Date())) return;
        invite.setStatus("expired");
        invite.setHandledAt(new Date());
        friendInviteMapper.updateById(invite);
    }

    /**
     * 处理 expirePendingInvitesForUser 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of expirePendingInvitesForUser with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
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
     * 查询并返回 getOnlineStatus 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getOnlineStatus with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    public String getOnlineStatus(Integer userId) {
        return WebSocketServer.getUserOnlineStatus(userId);
    }

    /**
     * 构建或转换 buildFriendView 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildFriendView with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param relation 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
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
     * 构建或转换 buildRequestView 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildRequestView with controlled input and output handling.
     *
     * @param request 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
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
     * 构建或转换 buildInviteView 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildInviteView with controlled input and output handling.
     *
     * @param invite 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
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
        item.put("map_id", invite.getMapId());
        item.put("map_name", invite.getMapId() == null ? "随机地图" : "地图 #" + invite.getMapId());
        item.put("room_name", invite.getRoomName());
        item.put("round_seconds", invite.getRoundSeconds());
        item.put("allow_spectator", Boolean.TRUE.equals(invite.getAllowSpectator()));
        item.put("status", invite.getStatus());
        item.put("created_at", DateTimeUtil.format(invite.getCreatedAt()));
        item.put("expired_at", DateTimeUtil.format(invite.getExpiredAt()));
        return item;
    }

    /**
     * 发送或通知 notifyFriendRequestReceived 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of notifyFriendRequestReceived with controlled input and output handling.
     *
     * @param request 输入参数；Input parameter.
     */
    public void notifyFriendRequestReceived(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-received");
        event.put("request", buildRequestView(request));
        WebSocketServer.sendEvent(request.getReceiverId(), event);
    }

    /**
     * 发送或通知 notifyFriendRequestHandled 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of notifyFriendRequestHandled with controlled input and output handling.
     *
     * @param request 输入参数；Input parameter.
     */
    public void notifyFriendRequestHandled(FriendRequest request) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-request-handled");
        event.put("request_id", request.getId());
        event.put("status", request.getStatus());
        WebSocketServer.sendEvent(request.getSenderId(), event);
    }

    /**
     * 发送或通知 notifyInviteReceived 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of notifyInviteReceived with controlled input and output handling.
     *
     * @param invite 输入参数；Input parameter.
     */
    public void notifyInviteReceived(FriendInvite invite) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-invite-received");
        event.put("invite", buildInviteView(invite));
        WebSocketServer.sendEvent(invite.getReceiverId(), event);
    }

    /**
     * 发送或通知 notifyInviteUpdated 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of notifyInviteUpdated with controlled input and output handling.
     *
     * @param invite 输入参数；Input parameter.
     * @param targetUserId 标识参数；Identifier value.
     */
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

    /**
     * 查询并返回 getMatchCount 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getMatchCount with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param friendId 标识参数；Identifier value.
     * @return 返回数值结果；Returns a numeric result.
     */
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
