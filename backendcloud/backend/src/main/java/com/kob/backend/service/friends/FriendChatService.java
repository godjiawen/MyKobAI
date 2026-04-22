package com.kob.backend.service.friends;

import java.util.Map;

/**
  * 定义 FriendChatService 的能力契约。
  * Defines the capability contract of Friend Chat Service.
 */
public interface FriendChatService {
    /**
     * 处理 conversations 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of conversations with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> conversations();

    /**
     * 处理 history 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of history with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> history(Integer friendId, Integer page, Integer pageSize);

    /**
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @param content 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> send(Integer friendId, String content);

    /**
     * 处理 read 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of read with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> read(Integer friendId);
}