package com.kob.backend.service.friends;

import java.util.Map;

/**
  * 定义 FriendInviteService 的能力契约。
  * Defines the capability contract of Friend Invite Service.
 */
public interface FriendInviteService {
    /**
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param receiverId 标识参数；Identifier value.
     * @param senderBotId 标识参数；Identifier value.
     * @param gameMode 对局相关参数；Game-related parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> send(Integer receiverId, Integer senderBotId, String gameMode);

    /**
     * 处理 respond 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of respond with controlled input and output handling.
     *
     * @param inviteId 标识参数；Identifier value.
     * @param action 输入参数；Input parameter.
     * @param receiverBotId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> respond(Integer inviteId, String action, Integer receiverBotId);

    /**
     * 处理 pending 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of pending with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> pending();
}