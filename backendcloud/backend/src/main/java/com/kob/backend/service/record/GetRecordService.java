package com.kob.backend.service.record;

import com.alibaba.fastjson.JSONObject;

/**
 * 服务接口定义。
 */
public interface GetRecordService {
    JSONObject getRecord(Integer recordId);
}
