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
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param receiverId 标识参数；Identifier value.
     * @param message 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
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
     * 处理 received 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of received with controlled input and output handling.
     *
     * @param status 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> received(String status, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", currentUser.getId())
                .eq(status != null && !status.isBlank(), "status", status)
                .orderByDesc("id");
        return listRequests(wrapper, page, pageSize);
    }

    /**
     * 处理 sent 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sent with controlled input and output handling.
     *
     * @param status 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> sent(String status, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", currentUser.getId())
                .eq(status != null && !status.isBlank(), "status", status)
                .orderByDesc("id");
        return listRequests(wrapper, page, pageSize);
    }

    /**
     * 处理 accept 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of accept with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
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
     * 处理 ignore 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of ignore with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
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
     * 删除或清理 cancel 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of cancel with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
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
     * 查询并返回 listRequests 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of listRequests with controlled input and output handling.
     *
     * @param wrapper 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
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
     * 处理 normalizeMessage 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of normalizeMessage with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) return "来打一局？";
        return message.trim().substring(0, Math.min(message.trim().length(), 100));
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