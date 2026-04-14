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
