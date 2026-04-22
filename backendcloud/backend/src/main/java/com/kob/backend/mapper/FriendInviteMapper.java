package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.FriendInvite;
import org.apache.ibatis.annotations.Mapper;

/**
  * 定义 FriendInviteMapper 的能力契约。
  * Defines the capability contract of Friend Invite Mapper.
 */
@Mapper
public interface FriendInviteMapper extends BaseMapper<FriendInvite> {
}
