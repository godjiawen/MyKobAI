package com.kob.botrunningsystem.service;

/**
 * 服务接口定义。
 */
public interface BotRunningService {
    String addBot(Integer userId, String botCode, String input, String language);
}
