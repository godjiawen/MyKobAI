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
     * Determines whether the snake tail should grow at the given step index.
     * 判断在给定步骤索引时蛇尾是否应继续增长。
     */
    private boolean check_tail_increasing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    /**
     * Computes and returns the list of cells occupied by this player's snake based on its steps.
     * 根据步骤列表计算并返回该玩家蛇身占据的格子列表。
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
     * Returns the steps list as a concatenated string of direction digits.
     * 将步骤列表作为方向数字拼接字符串返回。
     */
    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for (int d: steps) {
            res.append(d);
        }
        return res.toString();
    }
}
