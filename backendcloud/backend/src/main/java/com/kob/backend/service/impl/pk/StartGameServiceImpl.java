package com.kob.backend.service.impl.pk;

import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.service.pk.StartGameService;
import org.springframework.stereotype.Service;

/**
 * 服务实现类。
 */
@Service
public class StartGameServiceImpl implements StartGameService {
    /**
     * 创建或保存 startGame 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of startGame with controlled input and output handling.
     *
     * @param aId 标识参数；Identifier value.
     * @param aBotId 标识参数；Identifier value.
     * @param bId 标识参数；Identifier value.
     * @param bBotId 标识参数；Identifier value.
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId) {
        System.out.println("start game: " + aId + " " + bId);
        WebSocketServer.startGame(aId, aBotId, bId, bBotId);
        return "start game success";
    }
}