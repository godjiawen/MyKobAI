package com.kob.backend.service.friends;

import java.util.Map;

/**
  * 定义 FriendRequestService 的能力契约。
  * Defines the capability contract of Friend Request Service.
 */
public interface FriendRequestService {
    /**
     * 发送或通知 send 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of send with controlled input and output handling.
     *
     * @param receiverId 标识参数；Identifier value.
     * @param message 输入参数；Input parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> send(Integer receiverId, String message);

    /**
     * 处理 received 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of received with controlled input and output handling.
     *
     * @param status 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> received(String status, Integer page, Integer pageSize);

    /**
     * 处理 sent 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sent with controlled input and output handling.
     *
     * @param status 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> sent(String status, Integer page, Integer pageSize);

    /**
     * 处理 accept 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of accept with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> accept(Integer requestId);

    /**
     * 处理 ignore 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of ignore with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> ignore(Integer requestId);

    /**
     * 删除或清理 cancel 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of cancel with controlled input and output handling.
     *
     * @param requestId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> cancel(Integer requestId);
}