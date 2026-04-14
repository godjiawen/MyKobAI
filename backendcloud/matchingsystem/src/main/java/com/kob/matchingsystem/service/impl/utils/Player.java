package com.kob.matchingsystem.service.impl.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务实现类。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer userId;
    private Integer rating;
    private Integer botId;
    private Integer waitingTime;

}
