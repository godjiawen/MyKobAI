package com.kob.matchingsystem.service;

/**
 * 服务接口定义。
 */
public interface MatchingService {
    String addPlayer(Integer userId, Integer rating, Integer botId);
    String removePlayer(Integer userId);
}
