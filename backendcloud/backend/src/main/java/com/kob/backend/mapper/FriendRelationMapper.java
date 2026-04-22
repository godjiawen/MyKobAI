package com.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kob.backend.pojo.FriendRelation;
import org.apache.ibatis.annotations.Mapper;

/**
  * 定义 FriendRelationMapper 的能力契约。
  * Defines the capability contract of Friend Relation Mapper.
 */
@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {
}
