package com.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the game with position, steps, and optional bot settings.
 * 表示游戏中的玩家，包含位置、步骤及可选的机器人配置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id;
    private Integer botId; // -1 表示手动操作，否则表示由机器人代打。
    private String botCode;
    private String botLanguage;  // 机器人语言标识（四种语言）
    private Integer sx;
    private Integer sy;
    private List<Integer> steps;

    /**
     * 校验或判断 check_tail_increasing 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of check_tail_increasing with controlled input and output handling.
     *
     * @param step 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean check_tail_increasing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    /**
     * 查询并返回 getCells 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getCells with controlled input and output handling.
     *
     * @return 返回集合结果；Returns a collection result.
     */
    public List<Cell> getCells() {
        List<Cell> res = new ArrayList<>();

        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        int x = sx, y = sy;
        int step = 0;
        res.add(new Cell(x, y));
        for (int d: steps) {
            x += dx[d];
            y += dy[d];
            res.add(new Cell(x, y));
            if (!check_tail_increasing( ++ step )) {
                res.remove(0);
            }
        }
        return res;
    }

    /**
     * 查询并返回 getStepsString 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getStepsString with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for (int d: steps) {
            res.append(d);
        }
        return res.toString();
    }
}