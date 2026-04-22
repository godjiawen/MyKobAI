package com.kob.matchingsystem.service.impl;


import com.kob.matchingsystem.service.MatchingService;
import com.kob.matchingsystem.service.impl.utils.MatchingPool;
import org.springframework.stereotype.Service;

/**
 * 服务实现类。
 */
@Service
public class MatchingServiceImpl implements MatchingService {
    public final static MatchingPool matchingPool = new MatchingPool();

    /**
     * 创建或保存 addPlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addPlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param rating 输入参数；Input parameter.
     * @param botId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String addPlayer(Integer userId, Integer rating, Integer botId) {
        System.out.println("add player: " + userId + " " + rating);
        matchingPool.addPlayer(userId, rating, botId);
        return "add player success";
    }

    /**
     * 删除或清理 removePlayer 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of removePlayer with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String removePlayer(Integer userId) {
        System.out.println("remove player: " + userId);
        matchingPool.removePlayer(userId);
        return "remove player success";
    }
}