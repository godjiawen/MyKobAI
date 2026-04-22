package com.kob.backend.service.friends;

import java.util.Map;

/**
  * 定义 FriendService 的能力契约。
  * Defines the capability contract of Friend Service.
 */
public interface FriendService {
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
    Map<String, Object> list(Integer page, Integer pageSize, String keyword, String status, Boolean favoriteOnly, String sortBy);

    /**
     * 处理 stats 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of stats with controlled input and output handling.
     *
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> stats();

    /**
     * 查询并返回 search 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of search with controlled input and output handling.
     *
     * @param keyword 输入参数；Input parameter.
     * @param page 分页参数；Pagination parameter.
     * @param pageSize 分页参数；Pagination parameter.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> search(String keyword, Integer page, Integer pageSize);

    /**
     * 删除或清理 remove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of remove with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> remove(Integer friendId);

    /**
     * 更新 toggleFavorite 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of toggleFavorite with controlled input and output handling.
     *
     * @param friendId 标识参数；Identifier value.
     * @return 返回键值映射结果；Returns a key-value mapping result.
     */
    Map<String, Object> toggleFavorite(Integer friendId);
}