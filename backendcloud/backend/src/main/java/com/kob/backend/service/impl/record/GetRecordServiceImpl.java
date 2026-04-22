package com.kob.backend.service.impl.record;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.service.record.GetRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 服务实现类。
 */
@Service
public class GetRecordServiceImpl implements GetRecordService {
    @Autowired
    private RecordMapper recordMapper;

    /**
     * 查询并返回 getRecord 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getRecord with controlled input and output handling.
     *
     * @param recordId 标识参数；Identifier value.
     * @return 返回 JSONObject 类型结果；Returns a result of type JSONObject.
     */
    @Override
    public JSONObject getRecord(Integer recordId) {
        JSONObject resp = new JSONObject();

        if (recordId == null || recordId <= 0) {
            resp.put("error_message", "invalid record id");
            return resp;
        }

        Record record = recordMapper.selectById(recordId);
        if (record == null) {
            resp.put("error_message", "record not found");
            return resp;
        }

        resp.put("error_message", "success");
        resp.put("record", record);
        return resp;
    }
}