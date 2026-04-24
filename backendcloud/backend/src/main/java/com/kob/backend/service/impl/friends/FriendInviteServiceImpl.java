package com.kob.backend.service.impl.friends;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.consumer.utils.GameCreateOptions;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.FriendInviteMapper;
import com.kob.backend.pojo.Bot;
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
    @Autowired
    private BotMapper botMapper;

    /**
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param receiverId 标识参数；Identifier value.
     * @param senderBotId 标识参数；Identifier value.
     * @param gameMode 对局相关参数；Game-related parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
    public Map<String, Object> send(
            Integer receiverId,
            Integer senderBotId,
            String gameMode,
            Integer mapId,
            String roomName,
            Integer roundSeconds,
            Boolean allowSpectator) {
        User currentUser = friendDomainService.currentUser();
        if (receiverId == null) return error("receiver_id is required");
        if (!friendDomainService.areFriends(currentUser.getId(), receiverId)) return error("not your friend");
        if (currentUser.getId().equals(receiverId)) return error("cannot invite yourself");
        Integer normalizedSenderBotId = senderBotId == null ? -1 : senderBotId;
        if (!isBotOwnedBy(normalizedSenderBotId, currentUser.getId())) return error("sender bot not found");

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
                normalizedSenderBotId,
                null,
                mapId,
                normalizeRoomName(roomName),
                normalizeRoundSeconds(roundSeconds),
                allowSpectator == null || allowSpectator,
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

    /**
     * 处理 respond 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of respond with controlled input and output handling.
     *
     * @param inviteId 标识参数；Identifier value.
     * @param action 输入参数；Input parameter.
     * @param receiverBotId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
    @Transactional
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
            Integer normalizedReceiverBotId = receiverBotId == null ? -1 : receiverBotId;
            if (!isBotOwnedBy(normalizedReceiverBotId, currentUser.getId())) return error("receiver bot not found");
            invite.setStatus("accepted");
            invite.setReceiverBotId(normalizedReceiverBotId);
            invite.setHandledAt(new Date());
            friendInviteMapper.updateById(invite);
            friendDomainService.notifyInviteUpdated(invite, invite.getSenderId());
            friendDomainService.notifyInviteUpdated(invite, invite.getReceiverId());
            WebSocketServer.startGame(invite.getSenderId(), invite.getSenderBotId(), invite.getReceiverId(), invite.getReceiverBotId(), buildGameOptions(invite));

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
            match.put("match_type", "friend_private");
            match.put("map_id", invite.getMapId());
            match.put("map_name", buildMapName(invite.getMapId()));
            match.put("round_seconds", normalizeRoundSeconds(invite.getRoundSeconds()));
            match.put("allow_spectator", Boolean.TRUE.equals(invite.getAllowSpectator()));
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

    /**
     * 处理 pending 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of pending with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    @Override
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
     * 构建或转换 buildInviteOnlyResponse 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of buildInviteOnlyResponse with controlled input and output handling.
     *
     * @param invite 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
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

    private boolean isBotOwnedBy(Integer botId, Integer userId) {
        if (botId == null || botId.equals(-1)) return true;
        Bot bot = botMapper.selectById(botId);
        return bot != null && bot.getUserId().equals(userId);
    }

    private String normalizeRoomName(String roomName) {
        if (roomName == null) return "";
        String normalized = roomName.trim();
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private Integer normalizeRoundSeconds(Integer roundSeconds) {
        if (roundSeconds == null) return 15;
        if (roundSeconds < 10) return 10;
        if (roundSeconds > 30) return 30;
        return roundSeconds;
    }

    private String buildMapName(Integer mapId) {
        return mapId == null ? "随机地图" : "地图 #" + mapId;
    }

    private GameCreateOptions buildGameOptions(FriendInvite invite) {
        GameCreateOptions options = new GameCreateOptions();
        options.setMatchType("friend_private");
        options.setMapId(invite.getMapId());
        options.setMapName(buildMapName(invite.getMapId()));
        options.setRoundSeconds(normalizeRoundSeconds(invite.getRoundSeconds()));
        options.setAllowSpectator(invite.getAllowSpectator());
        options.setSourceType("friend_invite");
        options.setSourceId(invite.getId());
        return options;
    }
}
