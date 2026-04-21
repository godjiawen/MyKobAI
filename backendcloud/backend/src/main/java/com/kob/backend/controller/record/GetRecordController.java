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

    @GetMapping("/api/record/get/")
    /**
     * Handles getRecord.
     * ??getRecord?
     */
    public JSONObject getRecord(@RequestParam("record_id") Integer recordId) {
        return getRecordService.getRecord(recordId);
    }
}
