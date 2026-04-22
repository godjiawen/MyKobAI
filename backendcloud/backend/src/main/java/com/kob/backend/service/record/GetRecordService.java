package com.kob.backend.service.record;

import com.alibaba.fastjson.JSONObject;

/**
 * 服务接口定义。
 */
public interface GetRecordService {
    /**
     * 查询并返回 getRecord 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRecord with controlled input and output handling.
     *
     * @param recordId 标识参数；Identifier value.
     * @return 返回 JSONObject 类型结果；Returns a result of type JSONObject.
     */
    JSONObject getRecord(Integer recordId);
}