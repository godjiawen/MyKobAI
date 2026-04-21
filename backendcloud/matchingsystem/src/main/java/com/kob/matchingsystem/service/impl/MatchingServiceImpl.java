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

    @Override
    /**
     * Handles addPlayer.
     * ??addPlayer?
     */
    public String addPlayer(Integer userId, Integer rating, Integer botId) {
        System.out.println("add player: " + userId + " " + rating);
        matchingPool.addPlayer(userId, rating, botId);
        return "add player success";
    }

    @Override
    /**
     * Handles removePlayer.
     * ??removePlayer?
     */
    public String removePlayer(Integer userId) {
        System.out.println("remove player: " + userId);
        matchingPool.removePlayer(userId);
        return "remove player success";
    }
}
