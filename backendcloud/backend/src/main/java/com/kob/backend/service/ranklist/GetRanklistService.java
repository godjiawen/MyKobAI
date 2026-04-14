package com.kob.backend.service.ranklist;

import com.alibaba.fastjson.JSONObject;

/**
 * 服务接口定义。
 */
public interface GetRanklistService {
    JSONObject getList(Integer page);
}
