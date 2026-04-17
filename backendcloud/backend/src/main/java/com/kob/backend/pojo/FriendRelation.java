package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("friend_relation")
public class FriendRelation {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;
    private Integer friendId;
    private Boolean isFavorite;

    @TableField("created_at")
    private Date createdAt;
}
