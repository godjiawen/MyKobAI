package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.FriendChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FriendChatMessageMapper extends BaseMapper<FriendChatMessage> {
}
