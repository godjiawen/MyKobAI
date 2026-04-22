package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

/**
  * 定义 FriendRequestMapper 的能力契约。
  * Defines the capability contract of Friend Request Mapper.
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
}
