package com.kob.backend.service.impl.pk;

import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.service.pk.ReceiveBotMoveService;
import org.springframework.stereotype.Service;

/**
 * 服务实现类。
 */
@Service
public class ReceiveBotMoveServiceImpl implements ReceiveBotMoveService {
    /**
     * 处理 receiveBotMove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of receiveBotMove with controlled input and output handling.
     *
     * @param userId 标识参数；Identifier value.
     * @param direction 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    @Override
    public String receiveBotMove(Integer userId, Integer direction) {
        System.out.println("receive bot move: " + userId + " " + direction + " ");
        if (WebSocketServer.users.get(userId) != null) {
            Game game = WebSocketServer.users.get(userId).game;
            if (game != null) {
                if (game.getPlayerA().getId().equals(userId)) {
                    game.setNextStepA(direction);
                } else if (game.getPlayerB().getId().equals(userId)) {
                    game.setNextStepB(direction);
                }
            }
        }
        return "receive bot move success";
    }
}