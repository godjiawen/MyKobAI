package com.kob.botrunningsystem.service.impl;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.impl.utils.BotPool;
import org.springframework.stereotype.Service;

/**
 * 服务实现类。
 */
@Service
public class BotRunningServiceImpl implements BotRunningService {
    public final static BotPool botpool = new BotPool();

    @Override
    /**
     * Handles addBot.
     * ??addBot?
     */
    public String addBot(Integer userId, String botCode, String input, String language) {
        System.out.println("add bot: " + userId + " lang=" + language);
        botpool.addBot(userId, botCode, input, language);
        return "add bot success";
    }
}
