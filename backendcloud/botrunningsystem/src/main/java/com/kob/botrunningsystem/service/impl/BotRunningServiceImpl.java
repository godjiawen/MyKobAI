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

    /**
     * 创建或保存 addBot 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of addBot with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param botCode 机器人相关参数；Bot-related parameter.
     * @param input 输入参数；Input parameter.
     * @param language 语言参数；Language parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String addBot(Integer userId, String botCode, String input, String language) {
        System.out.println("add bot: " + userId + " lang=" + language);
        botpool.addBot(userId, botCode, input, language);
        return "add bot success";
    }
}