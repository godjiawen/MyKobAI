package com.kob.backend.service.user.bot;

import com.kob.backend.pojo.Bot;

import java.util.List;

/**
 * 服务接口定义。
 */
public interface GetListService {
    /**
     * 查询并返回 getList 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getList with controlled input and output handling.
     *
     * @return 返回集合结果；Returns a collection result.
     */
    List<Bot> getList();
}