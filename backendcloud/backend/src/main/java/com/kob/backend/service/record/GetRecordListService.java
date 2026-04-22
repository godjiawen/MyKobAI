package com.kob.backend.service.record;

import com.alibaba.fastjson.JSONObject;

/**
 * 服务接口定义。
 */
public interface GetRecordListService {
    /**
     * 查询并返回 getList 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getList with controlled input and output handling.
     *
     * @param page 分页参数；Pagination parameter.
     * @return 返回 JSONObject 类型结果；Returns a result of type JSONObject.
     */
    JSONObject getList(Integer page);
}