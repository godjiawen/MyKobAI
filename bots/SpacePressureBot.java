import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

/**
 * SpacePressure Bot
 *
 * Input protocol:
 * map#a_sx#a_sy#(a_steps)#b_sx#b_sy#(b_steps)
 *
 * Strategy:
 * 1) Rebuild both snakes from step history.
 * 2) Enumerate legal moves.
 * 3) Score each move by:
 *    - reachable safe area (flood fill)
 *    - one-step lookahead survivability
 *    - pressure toward opponent head when safe
 */
class Bot implements java.util.function.Supplier<Integer> {
    static class Cell {
        int x, y;
        Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final int ROWS = 13;
    private static final int COLS = 14;
    private static final int[] DX = {-1, 0, 1, 0};
    private static final int[] DY = {0, 1, 0, -1};

    private static final int AREA_WEIGHT = 100;
    private static final int FUTURE_MOVE_WEIGHT = 20;
    private static final int OPP_PRESSURE_WEIGHT = 2;
    private static final int WALL_PENALTY = 3;

    /**
     * Handles isTailGrowing.
     * ??isTailGrowing?
     */
    private boolean isTailGrowing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    /**
     * Handles buildSnakeCells.
     * ??buildSnakeCells?
     */
    private List<Cell> buildSnakeCells(int sx, int sy, String steps) {
        List<Cell> cells = new ArrayList<>();
        cells.add(new Cell(sx, sy));

        int x = sx, y = sy;
        int step = 0;
        for (int i = 0; i < steps.length(); i++) {
            int dir = steps.charAt(i) - '0';
            x += DX[dir];
            y += DY[dir];
            cells.add(new Cell(x, y));
            step++;
            if (!isTailGrowing(step)) {
                cells.remove(0);
            }
        }
        return cells;
    }

    /**
     * Handles floodFillArea.
     * ??floodFillArea?
     */
    private int floodFillArea(int[][] g, int sx, int sy) {
        if (sx < 0 || sx >= ROWS || sy < 0 || sy >= COLS || g[sx][sy] != 0) return 0;
        boolean[][] vis = new boolean[ROWS][COLS];
        Queue<Cell> q = new ArrayDeque<>();
        q.offer(new Cell(sx, sy));
        vis[sx][sy] = true;

        int area = 0;
        while (!q.isEmpty()) {
            Cell cur = q.poll();
            area++;
            for (int d = 0; d < 4; d++) {
                int nx = cur.x + DX[d], ny = cur.y + DY[d];
                if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
                if (vis[nx][ny] || g[nx][ny] != 0) continue;
                vis[nx][ny] = true;
                q.offer(new Cell(nx, ny));
            }
        }
        return area;
    }

    /**
     * Handles countNextLegalMoves.
     * ??countNextLegalMoves?
     */
    private int countNextLegalMoves(int[][] g, int x, int y) {
        int cnt = 0;
        for (int d = 0; d < 4; d++) {
            int nx = x + DX[d], ny = y + DY[d];
            if (nx < 0 || nx >= ROWS || ny < 0 || ny >= COLS) continue;
            if (g[nx][ny] == 0) cnt++;
        }
        return cnt;
    }

    /**
     * Handles manhattan.
     * ??manhattan?
     */
    private int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * Handles inside.
     * ??inside?
     */
    private boolean inside(int x, int y) {
        return x >= 0 && x < ROWS && y >= 0 && y < COLS;
    }

    /**
     * Handles evaluateMove.
     * ??evaluateMove?
     */
    private int evaluateMove(int[][] g, int nx, int ny, Cell oppHead) {
        int area = floodFillArea(g, nx, ny);
        int futureMoves = countNextLegalMoves(g, nx, ny);
        int distToOpp = manhattan(nx, ny, oppHead.x, oppHead.y);
        int nearWall = (nx == 0 || nx == ROWS - 1 ? 1 : 0) + (ny == 0 || ny == COLS - 1 ? 1 : 0);

        return area * AREA_WEIGHT
                + futureMoves * FUTURE_MOVE_WEIGHT
                - distToOpp * OPP_PRESSURE_WEIGHT
                - nearWall * WALL_PENALTY;
    }

    /**
     * Handles nextMove.
     * ??nextMove?
     */
    private Integer nextMove(String input) {
        String[] parts = input.split("#");
        String map = parts[0];

        int meSx = Integer.parseInt(parts[1]);
        int meSy = Integer.parseInt(parts[2]);
        String meSteps = parts[3].substring(1, parts[3].length() - 1);

        int oppSx = Integer.parseInt(parts[4]);
        int oppSy = Integer.parseInt(parts[5]);
        String oppSteps = parts[6].substring(1, parts[6].length() - 1);

        int[][] g = new int[ROWS][COLS];
        for (int i = 0, k = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++, k++) {
                g[i][j] = map.charAt(k) == '1' ? 1 : 0;
            }
        }

        List<Cell> me = buildSnakeCells(meSx, meSy, meSteps);
        List<Cell> opp = buildSnakeCells(oppSx, oppSy, oppSteps);

        for (Cell c : me) g[c.x][c.y] = 1;
        for (Cell c : opp) g[c.x][c.y] = 1;

        Cell myHead = me.get(me.size() - 1);
        Cell oppHead = opp.get(opp.size() - 1);

        int bestDir = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int d = 0; d < 4; d++) {
            int nx = myHead.x + DX[d];
            int ny = myHead.y + DY[d];
            if (!inside(nx, ny) || g[nx][ny] != 0) continue;

            g[nx][ny] = 1; // one-step simulation occupancy
            int score = evaluateMove(g, nx, ny, oppHead);
            g[nx][ny] = 0;

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        // Fallback: first legal direction if scoring failed for any reason.
        if (bestDir == -1) {
            for (int d = 0; d < 4; d++) {
                int nx = myHead.x + DX[d];
                int ny = myHead.y + DY[d];
                if (inside(nx, ny) && g[nx][ny] == 0) return d;
            }
            return 0;
        }

        return bestDir;
    }

    @Override
    /**
     * Handles get.
     * ??get?
     */
    public Integer get() {
        try {
            Scanner sc = new Scanner(new File("input.txt"));
            String input = sc.next();
            sc.close();
            return nextMove(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
