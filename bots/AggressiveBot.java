import java.util.Scanner;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * Bot 2 —— BFS 追击 + 空间压制 Bot
 *
 * 策略：
 *   1. 用 BFS 计算我的头到对手头的最短路径。
 *   2. 优先选择能沿最短路径逼近对手的方向（压缩对手空间）。
 *   3. 如果所有逼近方向都会让自身生存空间 < 安全阈值（8格），
 *      则退化为 Flood Fill 贪生存（不主动送死）。
 *   4. 对手生存空间 < 我方生存空间的 1/2 时，切换为纯生存模式保住优势。
 *
 * 适合场景：开局/中局主动进攻压制，残局空间告急时自动切防守。
 */
public class Bot implements java.util.function.Supplier<Integer> {

    static final int ROWS = 13, COLS = 14;
    static final int[] DX = {-1, 0, 1, 0};
    static final int[] DY = {0, 1, 0, -1};
    static final int SAFE_THRESHOLD = 8; // 生存空间安全线

    // ------------------------------------------------------------------ 解析

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

    /** BFS 返回从 start 到 goal 的最短步数，不可达返回 Integer.MAX_VALUE */
    static int bfsDistance(int[][] grid, int[] start, int[] goal) {
        if (grid[start[0]][start[1]] != 0 || grid[goal[0]][goal[1]] != 0) return Integer.MAX_VALUE;
        boolean[][] vis = new boolean[ROWS][COLS];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{start[0], start[1], 0});
        vis[start[0]][start[1]] = true;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            if (cur[0] == goal[0] && cur[1] == goal[1]) return cur[2];
            for (int d = 0; d < 4; d++) {
                int nx = cur[0] + DX[d], ny = cur[1] + DY[d];
                if (nx >= 0 && nx < ROWS && ny >= 0 && ny < COLS
                        && grid[nx][ny] == 0 && !vis[nx][ny]) {
                    vis[nx][ny] = true;
                    q.add(new int[]{nx, ny, cur[2] + 1});
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    /** BFS 统计从起点可达的空格数量（Flood Fill） */
    static int floodFill(int[][] grid, int sx, int sy) {
        if (sx < 0 || sx >= ROWS || sy < 0 || sy >= COLS) return 0;
        if (grid[sx][sy] != 0) return 0;
        boolean[][] vis = new boolean[ROWS][COLS];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{sx, sy});
        vis[sx][sy] = true;
        int count = 0;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            count++;
            for (int d = 0; d < 4; d++) {
                int nx = cur[0] + DX[d], ny = cur[1] + DY[d];
                if (nx >= 0 && nx < ROWS && ny >= 0 && ny < COLS
                        && grid[nx][ny] == 0 && !vis[nx][ny]) {
                    vis[nx][ny] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ 选方向

    /** 纯 Flood Fill 贪生存：选生存空间最大的方向 */
    static int floodFillChoice(int[][] g, int[] head) {
        int bestDir = -1, bestSpace = -1;
        for (int d = 0; d < 4; d++) {
            int nx = head[0] + DX[d], ny = head[1] + DY[d];
            if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
            if (g[nx][ny] != 0) continue;
            int space = floodFill(g, nx, ny);
            if (space > bestSpace) {
                bestSpace = space;
                bestDir   = d;
            }
        }
        return bestDir == -1 ? 0 : bestDir;
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

        String[] parts   = input.split("#");
        String mapStr    = parts[0];
        int meSx         = Integer.parseInt(parts[1]);
        int meSy         = Integer.parseInt(parts[2]);
        String meSteps   = parts[3].replace("(", "").replace(")", "");
        int youSx        = Integer.parseInt(parts[4]);
        int youSy        = Integer.parseInt(parts[5]);
        String youSteps  = parts[6].replace("(", "").replace(")", "");

        // 2. 构建静态地图
        int[][] g = new int[ROWS][COLS];
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++)
                g[i][j] = mapStr.charAt(i * COLS + j) - '0';

        // 3. 标记蛇身
        List<int[]> meCells  = getCells(meSx,  meSy,  meSteps);
        List<int[]> youCells = getCells(youSx, youSy, youSteps);
        for (int[] c : meCells)  g[c[0]][c[1]] = 1;
        for (int[] c : youCells) g[c[0]][c[1]] = 1;

        int[] myHead  = meCells.get(meCells.size() - 1);
        int[] oppHead = youCells.get(youCells.size() - 1);

        // 4. 评估双方当前生存空间
        // 临时清空蛇头，以便 floodFill 能从蛇头出发
        g[myHead[0]][myHead[1]]   = 0;
        g[oppHead[0]][oppHead[1]] = 0;
        int mySpace  = floodFill(g, myHead[0],  myHead[1]);
        int oppSpace = floodFill(g, oppHead[0], oppHead[1]);
        g[myHead[0]][myHead[1]]   = 1;
        g[oppHead[0]][oppHead[1]] = 1;

        // 5. 如果对手空间已经 ≤ 我的一半 → 切防守，保住优势
        if (oppSpace > 0 && mySpace > 0 && oppSpace <= mySpace / 2) {
            g[myHead[0]][myHead[1]] = 0; // 让 floodFill 能起步
            int dir = floodFillChoice(g, myHead);
            g[myHead[0]][myHead[1]] = 1;
            return dir;
        }

        // 6. 计算当前我到对手头的 BFS 距离（追击基准）
        g[myHead[0]][myHead[1]]   = 0;
        g[oppHead[0]][oppHead[1]] = 0;
        int curDist = bfsDistance(g, myHead, oppHead);
        g[myHead[0]][myHead[1]]   = 1;
        g[oppHead[0]][oppHead[1]] = 1;

        // 7. 枚举方向：优先选能缩短到对手距离 且 自身生存空间足够的方向
        int bestDir = -1, bestSpace = -1;
        int bestDistToOpp = Integer.MAX_VALUE;

        for (int d = 0; d < 4; d++) {
            int nx = myHead[0] + DX[d];
            int ny = myHead[1] + DY[d];
            if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
            if (g[nx][ny] != 0) continue;

            // 走一步后的生存空间
            g[nx][ny] = 1;
            int space = floodFill(g, nx, ny);
            g[nx][ny] = 0;
            space = floodFill(g, nx, ny); // 以空格状态计（含当前格自身）

            // 走一步后到对手头的距离
            g[nx][ny] = 0;
            g[oppHead[0]][oppHead[1]] = 0;
            int newDist = bfsDistance(g, new int[]{nx, ny}, oppHead);
            g[oppHead[0]][oppHead[1]] = 1;
            g[nx][ny] = 1;

            // 空间安全 + 能逼近对手 → 追击候选
            boolean safeSpace  = space >= SAFE_THRESHOLD;
            boolean approaching = newDist != Integer.MAX_VALUE && newDist <= curDist;

            if (safeSpace && approaching) {
                // 追击候选中：距离更近 优先；距离同则空间更大 优先
                if (newDist < bestDistToOpp
                        || (newDist == bestDistToOpp && space > bestSpace)) {
                    bestDistToOpp = newDist;
                    bestSpace     = space;
                    bestDir       = d;
                }
            }
        }

        // 8. 没有兼顾安全+追击的方向 → 退化为 Flood Fill 保命
        if (bestDir == -1) {
            g[myHead[0]][myHead[1]] = 0;
            bestDir = floodFillChoice(g, myHead);
            g[myHead[0]][myHead[1]] = 1;
        }

        return bestDir == -1 ? 0 : bestDir;
    }
}

