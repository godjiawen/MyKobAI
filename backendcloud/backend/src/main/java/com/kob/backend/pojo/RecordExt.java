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
@TableName("record_ext")
public class RecordExt {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer recordId;
    private String matchType;
    private Integer mapId;
    private String mapName;
    private Integer sourceId;
    private String sourceType;
    private Boolean allowSpectator;

    @TableField("created_at")
    private Date createdAt;
}
