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
@TableName("friend_request")
public class FriendRequest {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer senderId;
    private Integer receiverId;
    private String message;
    private String status;

    @TableField("created_at")
    private Date createdAt;

    @TableField("handled_at")
    private Date handledAt;
}
