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
     * Returns a filtered and paginated list of friends for the current user.
     * 返回当前用户经过筛选和分页处理的好友列表。
     */
    @Override
    /**
     * Handles list.
     * ??list?
     */
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
     * Returns friend statistics including counts of total, online, pending requests and new friends today.
     * 返回好友统计信息，包括总数、在线数、待处理请求数及今日新增好友数。
     */
    @Override
    /**
     * Handles stats.
     * ??stats?
     */
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
     * Searches all users by keyword (username or ID) and returns relation status for each.
     * 按关键字（用户名或ID）搜索所有用户并返回各用户的关系状态。
     */
    @Override
    /**
     * Handles search.
     * ??search?
     */
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
     * Removes the bidirectional friend relation between the current user and the specified friend.
     * 删除当前用户与指定好友之间的双向好友关系。
     */
    @Override
    @Transactional
    /**
     * Handles remove.
     * ??remove?
     */
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
     * Toggles the favorite status for the specified friend of the current user.
     * 切换当前用户对指定好友的收藏状态。
     */
    @Override
    @Transactional
    /**
     * Handles toggleFavorite.
     * ??toggleFavorite?
     */
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
     * Returns true if the friend item matches the given keyword in username or ID.
     * 若好友条目的用户名或ID匹配给定关键字则返回true。
     */
    private boolean matchesKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String normalized = keyword.trim().toLowerCase();
        return String.valueOf(item.get("friend_username")).toLowerCase().contains(normalized)
                || String.valueOf(item.get("friend_id")).contains(normalized);
    }

    /**
     * Returns true if the friend item's online status matches the given status filter.
     * 若好友条目的在线状态与给定过滤状态匹配则返回true。
     */
    private boolean matchesStatus(Map<String, Object> item, String status) {
        if (status == null || status.trim().isEmpty()) return true;
        return status.equals(item.get("online_status"));
    }

    /**
     * Returns true if the given date falls within today.
     * 若给定日期在今天范围内则返回true。
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
     * Returns true if the given string can be parsed as an integer.
     * 若给定字符串可被解析为整数则返回true。
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
     * Returns a success response map.
     * 返回成功响应Map。
     */
    private Map<String, Object> success() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", "success");
        return resp;
    }

    /**
     * Returns an error response map with the specified message.
     * 返回包含指定消息的错误响应Map。
     */
    private Map<String, Object> error(String errorMessage) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error_message", errorMessage);
        return resp;
    }
}
