package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.FriendChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
  * 定义 FriendChatMessageMapper 的能力契约。
  * Defines the capability contract of Friend Chat Message Mapper.
 */
@Mapper
public interface FriendChatMessageMapper extends BaseMapper<FriendChatMessage> {
}
