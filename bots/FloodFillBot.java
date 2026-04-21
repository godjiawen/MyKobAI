import java.util.Scanner;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * Bot 1 —— 洪水填充（Flood Fill）贪生存 Bot
 *
 * 策略：
 *   每一步枚举 4 个方向，对每个合法方向做一次 BFS 统计
 *   从该方向的落点出发能到达多少个空格（即"生存空间"）。
 *   选择生存空间最大的方向走。
 *   同分时优先选离对手头部更远的方向（逃跑倾向）。
 *
 * 适合场景：残局空间紧张时存活率极高，但进攻性弱。
 */
public class Bot implements java.util.function.Supplier<Integer> {

    static final int ROWS = 13, COLS = 14;
    static final int[] DX = {-1, 0, 1, 0};
    static final int[] DY = {0, 1, 0, -1};

    // ------------------------------------------------------------------ 解析

    /** 从起点 + 历史步骤重建蛇身，返回格子列表（index 0 = 蛇尾，最后 = 蛇头）*/
    static List<int[]> getCells(int sx, int sy, String steps) {
        List<int[]> cells = new ArrayList<>();
        cells.add(new int[]{sx, sy});
        int x = sx, y = sy, step = 0;
        for (int i = 0; i < steps.length(); i++) {
            int d = steps.charAt(i) - '0';
            x += DX[d]; y += DY[d];
            cells.add(new int[]{x, y});
            step++;
            boolean growing = (step <= 10) || (step % 3 == 1);
            if (!growing) cells.remove(0);
        }
        return cells;
    }

    // ------------------------------------------------------------------ 工具

    /** BFS 统计从 (startX, startY) 出发在 grid 上能到达的空格数量 */
    static int floodFill(int[][] grid, int startX, int startY) {
        if (grid[startX][startY] != 0) return 0;
        boolean[][] visited = new boolean[ROWS][COLS];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        int count = 0;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            count++;
            for (int d = 0; d < 4; d++) {
                int nx = cur[0] + DX[d], ny = cur[1] + DY[d];
                if (nx >= 0 && nx < ROWS && ny >= 0 && ny < COLS
                        && grid[nx][ny] == 0 && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return count;
    }

    /** 曼哈顿距离 */
    static int dist(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    // ------------------------------------------------------------------ 主逻辑

    @Override
    /**
     * Handles get.
     * ??get?
     */
    public Integer get() {
        // 1. 读取输入
        Scanner sc;
        try {
            sc = new Scanner(new java.io.File("input.txt"));
        } catch (Exception e) {
            return 0;
        }
        String input = sc.next();
        sc.close();

        String[] parts = input.split("#");
        String mapStr   = parts[0];
        int meSx        = Integer.parseInt(parts[1]);
        int meSy        = Integer.parseInt(parts[2]);
        String meSteps  = parts[3].replace("(", "").replace(")", "");
        int youSx       = Integer.parseInt(parts[4]);
        int youSy       = Integer.parseInt(parts[5]);
        String youSteps = parts[6].replace("(", "").replace(")", "");

        // 2. 构建地图（静态障碍）
        int[][] g = new int[ROWS][COLS];
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++)
                g[i][j] = mapStr.charAt(i * COLS + j) - '0';

        // 3. 标记蛇身为障碍
        List<int[]> meCells  = getCells(meSx,  meSy,  meSteps);
        List<int[]> youCells = getCells(youSx, youSy, youSteps);
        for (int[] c : meCells)  g[c[0]][c[1]] = 1;
        for (int[] c : youCells) g[c[0]][c[1]] = 1;

        // 4. 当前蛇头 & 对手蛇头
        int[] myHead  = meCells.get(meCells.size() - 1);
        int[] oppHead = youCells.get(youCells.size() - 1);

        // 5. 枚举方向，选生存空间最大的
        int bestDir = -1, bestSpace = -1, bestDistToOpp = -1;

        for (int d = 0; d < 4; d++) {
            int nx = myHead[0] + DX[d];
            int ny = myHead[1] + DY[d];

            // 越界或撞墙/身体 → 跳过
            if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
            if (g[nx][ny] != 0) continue;

            // 临时占格，模拟走一步后的地图
            g[nx][ny] = 1;
            int space = floodFill(g, nx, ny); // 注意：起点已被标为1，所以先还原再BFS
            g[nx][ny] = 0;                    // 还原
            space = floodFill(g, nx, ny);     // 以起点空格状态做BFS（起点自身也算1格）

            int distToOpp = dist(nx, ny, oppHead[0], oppHead[1]);

            // 优先更大空间；同分时选离对手更远的（保守策略）
            if (space > bestSpace || (space == bestSpace && distToOpp > bestDistToOpp)) {
                bestSpace      = space;
                bestDistToOpp  = distToOpp;
                bestDir        = d;
            }
        }

        return bestDir == -1 ? 0 : bestDir;
    }
}

