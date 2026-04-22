package com.kob.backend.service.impl.friends;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.FriendRequestMapper;
import com.kob.backend.mapper.FriendRelationMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.FriendRelation;
import com.kob.backend.pojo.User;
import com.kob.backend.service.friends.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for managing friend list operations: listing, stats, search, remove and favorite toggle.
 * 好友列表操作服务实现：列表查询、统计、搜索、删除和收藏切换。
 */
@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    private FriendRelationMapper friendRelationMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendDomainService friendDomainService;

    /**
     * 查询并返回 list 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of list with controlled input and output handling.
     *
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @param keyword 输入参数；Input parameter.
     * @param status 输入参数；Input parameter.
     * @param favoriteOnly 输入参数；Input parameter.
     * @param sortBy 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> list(Integer page, Integer pageSize, String keyword, String status, Boolean favoriteOnly, String sortBy) {
        User currentUser = friendDomainService.currentUser();
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUser.getId());
        if (Boolean.TRUE.equals(favoriteOnly)) {
            wrapper.eq("is_favorite", true);
        }
        if ("created_at_desc".equals(sortBy)) {
            wrapper.orderByDesc("created_at");
        } else {
            wrapper.orderByDesc("id");
        }

        List<FriendRelation> relations = friendRelationMapper.selectList(wrapper);
        List<Map<String, Object>> filteredFriends = new ArrayList<>();
        for (FriendRelation relation : relations) {
            Map<String, Object> item = friendDomainService.buildFriendView(currentUser.getId(), relation);
            if (matchesKeyword(item, keyword) && matchesStatus(item, status)) {
                filteredFriends.add(item);
            }
        }

        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, filteredFriends.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, filteredFriends.size());
        List<Map<String, Object>> friends = new ArrayList<>(filteredFriends.subList(fromIndex, toIndex));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        resp.put("friends", friends);
        resp.put("friends_count", filteredFriends.size());
        return resp;
    }

    /**
     * 处理 stats 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of stats with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> stats() {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<FriendRelation> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("user_id", currentUser.getId());
        List<FriendRelation> relations = friendRelationMapper.selectList(relationWrapper);

        long onlineCount = relations.stream()
                .map(FriendRelation::getFriendId)
                .filter(friendId -> !"offline".equals(friendDomainService.getOnlineStatus(friendId)))
                .count();

        QueryWrapper<com.kob.backend.pojo.FriendRequest> receivedWrapper = new QueryWrapper<>();
        receivedWrapper.eq("receiver_id", currentUser.getId()).eq("status", "pending");
        QueryWrapper<com.kob.backend.pojo.FriendRequest> sentWrapper = new QueryWrapper<>();
        sentWrapper.eq("sender_id", currentUser.getId()).eq("status", "pending");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("friends_count", relations.size());
        stats.put("online_count", onlineCount);
        stats.put("pending_received_count", friendRequestMapper.selectCount(receivedWrapper));
        stats.put("pending_sent_count", friendRequestMapper.selectCount(sentWrapper));
        stats.put("today_new_count", relations.stream().filter(relation -> isToday(relation.getCreatedAt())).count());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        resp.put("stats", stats);
        return resp;
    }

    /**
     * 查询并返回 search 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of search with controlled input and output handling.
     *
     * @param keyword 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    public Map<String, Object> search(String keyword, Integer page, Integer pageSize) {
        User currentUser = friendDomainService.currentUser();
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String normalized = keyword.trim();
            wrapper.and(query -> query.like("username", normalized).or().eq(isInteger(normalized), "id", normalized));
        }
        wrapper.orderByDesc("rating");

        IPage<User> userPage = new Page<>(page, pageSize);
        List<User> users = userMapper.selectPage(userPage, wrapper).getRecords();
        List<Map<String, Object>> items = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("user_id", user.getId());
            item.put("username", user.getUsername());
            item.put("photo", user.getPhoto());
            item.put("rating", user.getRating());
            item.put("online_status", friendDomainService.getOnlineStatus(user.getId()));
            item.put("relation_status", friendDomainService.getRelationStatus(currentUser.getId(), user.getId()));
            item.put("request_id", friendDomainService.getPendingRequestId(currentUser.getId(), user.getId()));
            items.add(item);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        resp.put("users", items);
        resp.put("users_count", userPage.getTotal());
        return resp;
    }

    /**
     * 删除或清理 remove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of remove with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
    public Map<String, Object> remove(Integer friendId) {
        User currentUser = friendDomainService.currentUser();
        if (friendId == null) return error("friend_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), friendId)) return error("friend not found");

        QueryWrapper<FriendRelation> left = new QueryWrapper<>();
        left.eq("user_id", currentUser.getId()).eq("friend_id", friendId);
        QueryWrapper<FriendRelation> right = new QueryWrapper<>();
        right.eq("user_id", friendId).eq("friend_id", currentUser.getId());
        friendRelationMapper.delete(left);
        friendRelationMapper.delete(right);
        return success();
    }

    /**
     * 更新 toggleFavorite 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of toggleFavorite with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(Integer friendId) {
        User currentUser = friendDomainService.currentUser();
        if (friendId == null) return error("friend_id is required");
        FriendRelation relation = friendDomainService.getRelation(currentUser.getId(), friendId);
        if (relation == null) return error("friend not found");
        relation.setIsFavorite(!Boolean.TRUE.equals(relation.getIsFavorite()));
        friendRelationMapper.updateById(relation);

        Map<String, Object> resp = success();
        resp.put("is_favorite", relation.getIsFavorite());
        return resp;
    }

    /**
     * 处理 matchesKeyword 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of matchesKeyword with controlled input and output handling.
     *
     * @param item 输入参数；Input parameter.
     * @param keyword 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean matchesKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String normalized = keyword.trim().toLowerCase();
        return String.valueOf(item.get("friend_username")).toLowerCase().contains(normalized)
                || String.valueOf(item.get("friend_id")).contains(normalized);
    }

    /**
     * 处理 matchesStatus 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of matchesStatus with controlled input and output handling.
     *
     * @param item 输入参数；Input parameter.
     * @param status 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean matchesStatus(Map<String, Object> item, String status) {
        if (status == null || status.trim().isEmpty()) return true;
        return status.equals(item.get("online_status"));
    }

    /**
     * 校验或判断 isToday 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isToday with controlled input and output handling.
     *
     * @param date 时间参数；Time parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean isToday(java.util.Date date) {
        if (date == null) return false;
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTime(date);
        return today.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR)
                && today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /**
     * 校验或判断 isInteger 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isInteger with controlled input and output handling.
     *
     * @param value 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception e) {
            return false;
        }
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