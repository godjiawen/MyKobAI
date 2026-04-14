package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.Bot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据访问映射接口。
 */
@Mapper
public interface BotMapper extends BaseMapper<Bot> {
}
