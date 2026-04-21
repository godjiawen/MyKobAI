package com.kob.backend.service.impl.friends;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.FriendRelationMapper;
import com.kob.backend.mapper.FriendRequestMapper;
import com.kob.backend.pojo.FriendRelation;
import com.kob.backend.pojo.FriendRequest;
import com.kob.backend.pojo.User;
import com.kob.backend.service.friends.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for managing friend requests (send, receive, accept, ignore, cancel).
 * 好友请求管理服务实现，支持发送、接收、接受、忽略和取消操作。
 */
@Service
public class FriendRequestServiceImpl implements FriendRequestService {
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private FriendDomainService friendDomainService;

    /**
     * Sends a friend request from the current user to the specified receiver.
     * 从当前用户向指定接收者发送好友请求。
     */
    @Override
    @Transactional
    /**
     * Handles send.
     * ??send?
     */
    public Map<String, Object> send(Integer receiverId, String message) {
        User currentUser = friendDomainService.currentUser();
        if (receiverId == null) return error("receiver_id is required");
        if (currentUser.getId().equals(receiverId)) return error("cannot add yourself");
        if (friendDomainService.areFriends(currentUser.getId(), receiverId)) return error("already friends");
        if (friendDomainService.getPendingRequest(currentUser.getId(), receiverId) != null) return error("request already sent");
        if (friendDomainService.getPendingRequest(receiverId, currentUser.getId()) != null) return error("request already received");

        FriendRequest request = new FriendRequest(
                null,
                currentUser.getId(),
                receiverId,
                normalizeMessage(message),
                "pending",
                new Date(),
                null
        );
        friendRequestMapper.insert(request);
        friendDomainService.notifyFriendRequestReceived(request);

        Map<String, Object> resp = success();
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("request_id", request.getId());
        requestData.put("status", request.getStatus());
        resp.put("request", requestData);
        return resp;
    }

    /**
     * Returns a paginated list of friend requests received by the current user.
     * 返回当前用户收到的好友请求的分页列表。
     */
    @Override
    /**
     * Handles received.
     * ??received?
     */
    public Map<String, Object> received(String status, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", currentUser.getId())
                .eq(status != null && !status.isBlank(), "status", status)
                .orderByDesc("id");
        return listRequests(wrapper, page, pageSize);
    }

    /**
     * Returns a paginated list of friend requests sent by the current user.
     * 返回当前用户发出的好友请求的分页列表。
     */
    @Override
    /**
     * Handles sent.
     * ??sent?
     */
    public Map<String, Object> sent(String status, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", currentUser.getId())
                .eq(status != null && !status.isBlank(), "status", status)
                .orderByDesc("id");
        return listRequests(wrapper, page, pageSize);
    }

    /**
     * Accepts a pending friend request and creates the bidirectional friend relations.
     * 接受待处理的好友请求并创建双向好友关系。
     */
    @Override
    @Transactional
    /**
     * Handles accept.
     * ??accept?
     */
    public Map<String, Object> accept(Integer requestId) {
        User currentUser = friendDomainService.currentUser();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) return error("request not found");
        if (!currentUser.getId().equals(request.getReceiverId())) return error("no permission");
        if (!"pending".equals(request.getStatus())) return error("request already handled");

        request.setStatus("accepted");
        request.setHandledAt(new Date());
        friendRequestMapper.updateById(request);

        Date now = new Date();
        friendRelationMapper.insert(new FriendRelation(null, request.getSenderId(), request.getReceiverId(), false, now));
        friendRelationMapper.insert(new FriendRelation(null, request.getReceiverId(), request.getSenderId(), false, now));
        friendDomainService.notifyFriendRequestHandled(request);

        Map<String, Object> resp = success();
        FriendRelation relation = friendDomainService.getRelation(currentUser.getId(), request.getSenderId());
        resp.put("friend", friendDomainService.buildFriendView(currentUser.getId(), relation));
        return resp;
    }

    /**
     * Ignores (soft-declines) a pending friend request.
     * 忽略（软拒绝）一个待处理的好友请求。
     */
    @Override
    @Transactional
    /**
     * Handles ignore.
     * ??ignore?
     */
    public Map<String, Object> ignore(Integer requestId) {
        User currentUser = friendDomainService.currentUser();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) return error("request not found");
        if (!currentUser.getId().equals(request.getReceiverId())) return error("no permission");
        if (!"pending".equals(request.getStatus())) return error("request already handled");

        request.setStatus("ignored");
        request.setHandledAt(new Date());
        friendRequestMapper.updateById(request);
        friendDomainService.notifyFriendRequestHandled(request);
        return success();
    }

    /**
     * Cancels a pending friend request previously sent by the current user.
     * 取消当前用户此前发出的待处理好友请求。
     */
    @Override
    @Transactional
    /**
     * Handles cancel.
     * ??cancel?
     */
    public Map<String, Object> cancel(Integer requestId) {
        User currentUser = friendDomainService.currentUser();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) return error("request not found");
        if (!currentUser.getId().equals(request.getSenderId())) return error("no permission");
        if (!"pending".equals(request.getStatus())) return error("request already handled");

        request.setStatus("cancelled");
        request.setHandledAt(new Date());
        friendRequestMapper.updateById(request);
        return success();
    }

    /**
     * Retrieves a paginated list of friend requests matching the given query wrapper.
     * 检索满足给定查询条件的好友请求分页列表。
     */
    private Map<String, Object> listRequests(QueryWrapper<FriendRequest> wrapper, Integer page, Integer pageSize) {
        IPage<FriendRequest> requestPage = new Page<>(page, pageSize);
        List<FriendRequest> requests = friendRequestMapper.selectPage(requestPage, wrapper).getRecords();
        List<Map<String, Object>> items = new ArrayList<>();
        for (FriendRequest request : requests) {
            items.add(friendDomainService.buildRequestView(request));
        }
        Map<String, Object> resp = success();
        resp.put("requests", items);
        resp.put("requests_count", requestPage.getTotal());
        return resp;
    }

    /**
     * Normalizes the request message, trimming and truncating to 100 characters.
     * 标准化请求消息，去除首尾空格并截断至100个字符。
     */
    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) return "来打一局？";
        return message.trim().substring(0, Math.min(message.trim().length(), 100));
    }

    /**
     * Returns a success response map with error_message set to "success".
     * 返回error_message为"success"的成功响应Map。
     */
    private Map<String, Object> success() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        return resp;
    }

    /**
     * Returns an error response map with the specified error message.
     * 返回包含指定错误信息的错误响应Map。
     */
    private Map<String, Object> error(String errorMessage) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", errorMessage);
        return resp;
    }
}
