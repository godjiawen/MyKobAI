package com.kob.backend.controller.record;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.service.record.GetRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制器，负责接收请求并调用服务层。
 */
@RestController
public class GetRecordController {
    @Autowired
    private GetRecordService getRecordService;

    /**
     * 处理 RequestParam 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of RequestParam with controlled input and output handling.
     *
     * @param recordId 标识参数；Identifier value.
     * @return 返回 JSONObject getRecord(@ 类型结果；Returns a result of type JSONObject getRecord(@.
     */
    @GetMapping("/api/record/get/")
    public JSONObject getRecord(@RequestParam("record_id") Integer recordId) {
        return getRecordService.getRecord(recordId);
    }
}