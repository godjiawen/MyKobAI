package com.kob.backend.service.impl.friends;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.mapper.FriendChatMessageMapper;
import com.kob.backend.mapper.FriendRelationMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.FriendChatMessage;
import com.kob.backend.pojo.FriendRelation;
import com.kob.backend.pojo.User;
import com.kob.backend.service.friends.FriendChatService;
import com.kob.backend.utils.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendChatServiceImpl implements FriendChatService {
    @Autowired
    private FriendChatMessageMapper friendChatMessageMapper;
    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendDomainService friendDomainService;

    /**
     * 处理 conversations 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of conversations with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> conversations() {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRelation> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("user_id", currentUser.getId()).orderByDesc("created_at");
        List<FriendRelation> relations = friendRelationMapper.selectList(relationWrapper);

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (FriendRelation relation : relations) {
            User friend = userMapper.selectById(relation.getFriendId());
            if (friend == null) continue;

            FriendChatMessage latestMessage = friendChatMessageMapper.selectOne(buildConversationWrapper(currentUser.getId(), friend.getId())
                    .orderByDesc("id")
                    .last("limit 1"));

            QueryWrapper<FriendChatMessage> unreadWrapper = new QueryWrapper<>();
            unreadWrapper.eq("sender_id", friend.getId())
                    .eq("receiver_id", currentUser.getId())
                    .eq("is_read", false);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("friend_id", friend.getId());
            item.put("friend_username", friend.getUsername());
            item.put("friend_photo", friend.getPhoto());
            item.put("friend_rating", friend.getRating());
            item.put("online_status", friendDomainService.getOnlineStatus(friend.getId()));
            item.put("last_message", latestMessage != null ? latestMessage.getContent() : "");
            item.put("last_message_sender_id", latestMessage != null ? latestMessage.getSenderId() : null);
            item.put("last_message_at", DateTimeUtil.format(latestMessage != null ? latestMessage.getCreatedAt() : null));
            item.put("unread_count", friendChatMessageMapper.selectCount(unreadWrapper));
            conversations.add(item);
        }

        conversations.sort((left, right) -> {
            String leftTime = String.valueOf(left.get("last_message_at"));
            String rightTime = String.valueOf(right.get("last_message_at"));
            if (rightTime != null && !rightTime.isBlank() && (leftTime == null || leftTime.isBlank())) return 1;
            if (leftTime != null && !leftTime.isBlank() && (rightTime == null || rightTime.isBlank())) return -1;
            return rightTime.compareTo(leftTime);
        });

        Map<String, Object> resp = success();
        resp.put("conversations", conversations);
        return resp;
    }

    /**
     * 处理 history 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of history with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> history(Integer friendId, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        if (friendId == null) return error("friend_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), friendId)) return error("friend not found");

        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 50 : pageSize;

        IPage<FriendChatMessage> messagePage = new Page<>(normalizedPage, normalizedPageSize);
        QueryWrapper<FriendChatMessage> wrapper = buildConversationWrapper(currentUser.getId(), friendId).orderByDesc("id");
        List<FriendChatMessage> records = friendChatMessageMapper.selectPage(messagePage, wrapper).getRecords();
        Collections.reverse(records);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (FriendChatMessage record : records) {
            messages.add(buildMessageView(record));
        }

        markConversationAsRead(currentUser.getId(), friendId);

        Map<String, Object> resp = success();
        resp.put("messages", messages);
        resp.put("messages_count", messagePage.getTotal());
        return resp;
    }

    /**
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @param content 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
    public Map<String, Object> send(Integer friendId, String content) {
        User currentUser = friendDomainService.currentUser();
        if (friendId == null) return error("friend_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), friendId)) return error("friend not found");
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) return error("content is required");
        if (normalizedContent.length() > 500) return error("content is too long");

        FriendChatMessage message = new FriendChatMessage(
                null,
                currentUser.getId(),
                friendId,
                normalizedContent,
                false,
                new Date()
        );
        friendChatMessageMapper.insert(message);

        Map<String, Object> messageView = buildMessageView(message);
        notifyChatMessage(currentUser.getId(), messageView);
        notifyChatMessage(friendId, messageView);

        Map<String, Object> resp = success();
        resp.put("message", messageView);
        return resp;
    }

    /**
     * 处理 read 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of read with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
    public Map<String, Object> read(Integer friendId) {
        User currentUser = friendDomainService.currentUser();
        if (friendId == null) return error("friend_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), friendId)) return error("friend not found");
        markConversationAsRead(currentUser.getId(), friendId);
        return success();
    }

    /**
     * 构建或转换 buildConversationWrapper 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildConversationWrapper with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param friendId 标识参数；Identifier value.
     * @return 返回 QueryWrapper<FriendChatMessage> 类型结果；Returns a result of type QueryWrapper<FriendChatMessage>.
     */
    private QueryWrapper<FriendChatMessage> buildConversationWrapper(Integer currentUserId, Integer friendId) {
        QueryWrapper<FriendChatMessage> wrapper = new QueryWrapper<>();
        wrapper.and(query -> query
                .and(item -> item.eq("sender_id", currentUserId).eq("receiver_id", friendId))
                .or(item -> item.eq("sender_id", friendId).eq("receiver_id", currentUserId)));
        return wrapper;
    }

    /**
     * 构建或转换 buildMessageView 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildMessageView with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    private Map<String, Object> buildMessageView(FriendChatMessage message) {
        User sender = userMapper.selectById(message.getSenderId());
        User receiver = userMapper.selectById(message.getReceiverId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("message_id", message.getId());
        item.put("sender_id", message.getSenderId());
        item.put("sender_username", sender != null ? sender.getUsername() : "(已注销)");
        item.put("sender_photo", sender != null ? sender.getPhoto() : "");
        item.put("receiver_id", message.getReceiverId());
        item.put("receiver_username", receiver != null ? receiver.getUsername() : "(已注销)");
        item.put("receiver_photo", receiver != null ? receiver.getPhoto() : "");
        item.put("content", message.getContent());
        item.put("is_read", Boolean.TRUE.equals(message.getIsRead()));
        item.put("created_at", DateTimeUtil.format(message.getCreatedAt()));
        return item;
    }

    /**
     * 发送或通知 notifyChatMessage 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of notifyChatMessage with controlled input and output handling.
     *
     * @param targetUserId 标识参数；Identifier value.
     * @param message 输入参数；Input parameter.
     */
    private void notifyChatMessage(Integer targetUserId, Map<String, Object> message) {
        JSONObject event = new JSONObject();
        event.put("event", "friend-chat-message");
        event.put("message", message);
        WebSocketServer.sendEvent(targetUserId, event);
    }

    /**
     * 处理 markConversationAsRead 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of markConversationAsRead with controlled input and output handling.
     *
     * @param currentUserId 标识参数；Identifier value.
     * @param friendId 标识参数；Identifier value.
     */
    private void markConversationAsRead(Integer currentUserId, Integer friendId) {
        UpdateWrapper<FriendChatMessage> wrapper = new UpdateWrapper<>();
        wrapper.eq("sender_id", friendId)
                .eq("receiver_id", currentUserId)
                .eq("is_read", false)
                .set("is_read", true);
        friendChatMessageMapper.update(null, wrapper);
    }

    /**
     * 处理 success 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of success with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    private Map<String, Object> success() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        return resp;
    }

    /**
     * 处理 error 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of error with controlled input and output handling.
     *
     * @param errorMessage 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    private Map<String, Object> error(String errorMessage) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", errorMessage);
        return resp;
    }
}