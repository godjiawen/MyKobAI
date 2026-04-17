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
@TableName("friend_invite")
public class FriendInvite {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer senderId;
    private Integer receiverId;
    private String gameMode;
    private Integer senderBotId;
    private Integer receiverBotId;
    private String status;

    @TableField("created_at")
    private Date createdAt;

    @TableField("expired_at")
    private Date expiredAt;

    @TableField("handled_at")
    private Date handledAt;
}
