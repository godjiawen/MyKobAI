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
@TableName("friend_chat_message")
public class FriendChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer senderId;
    private Integer receiverId;
    private String content;
    private Boolean isRead;

    @TableField("created_at")
    private Date createdAt;
}
