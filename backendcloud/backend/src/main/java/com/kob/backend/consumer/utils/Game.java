package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.kob.backend.service.pk.GameSnapshotService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Game thread that controls game logic, map generation, step handling, judging and result saving.
 * 娓告垙绾跨▼锛屾帶鍒舵父鎴忛€昏緫銆佸湴鍥剧敓鎴愩€佹楠ゅ鐞嗐€佸垽灞€鍙婄粨鏋滀繚瀛樸€?
 */
public class Game extends Thread {
    private final static int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
    private static final int STEP_TIMEOUT_MS = 15_000;
    private static final int STEP_POLL_INTERVAL_MS = 100;
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_walls_count;
    private final int[][] g;
    private final Player playerA, playerB;

    private Integer nextStepA = null;
    private Integer nextStepB = null;
    private ReentrantLock lock = new ReentrantLock();
    private String status = "playing";
    private String loser = "";
    private volatile boolean paused = false;
    private volatile String pausedBy = "";

    // 鏂嚎閲嶈繛瀛楁
    private final String gameId;
    private final String roomId;
    private volatile boolean suspended = false;
    private volatile Integer suspendedBy = null;
    private volatile String suspendedReason = "";
    private final Set<Integer> awayPlayers = ConcurrentHashMap.newKeySet();
    public static GameSnapshotService gameSnapshotService;

    private final static String addBotUrl = "http://127.0.0.1:3002/bot/add/";

    /**
     * 处理 Game 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of Game with controlled input and output handling.
     *
     * @param rows 输入参数；Input parameter.
     * @param cols 输入参数；Input parameter.
     * @param inner_walls_count 输入参数；Input parameter.
     * @param idA 输入参数；Input parameter.
     * @param botA 机器人相关参数；Bot-related parameter.
     * @param idB 输入参数；Input parameter.
     * @param botB 机器人相关参数；Bot-related parameter.
     * @param gameId 标识参数；Identifier value.
     * @param roomId 标识参数；Identifier value.
     */
    public Game(Integer rows,
                Integer cols,
                Integer inner_walls_count,
                Integer idA,
                Bot botA,
                Integer idB,
                Bot botB,
                String gameId,
                String roomId
    ) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        this.gameId = gameId;
        this.roomId = roomId;

        Integer botIdA = -1, botIdB = -1;
        String botCodeA = "", botCodeB = "";
        String botLanguageA = "java", botLanguageB = "java";
        if (botA != null) {
            botIdA = botA.getId();
            botCodeA = botA.getContent();
            botLanguageA = botA.getLanguage() != null ? botA.getLanguage() : "java";
        }
        if (botB != null) {
            botIdB = botB.getId();
            botCodeB = botB.getContent();
            botLanguageB = botB.getLanguage() != null ? botB.getLanguage() : "java";
        }

        playerA = new Player(idA, botIdA, botCodeA, botLanguageA, rows - 2, 1, new ArrayList<>());
        playerB = new Player(idB, botIdB, botCodeB, botLanguageB, 1, cols - 2, new ArrayList<>());
    }

    /**
     * 查询并返回 getPlayerA 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPlayerA with controlled input and output handling.
     *
     * @return 返回 Player 类型结果；Returns a result of type Player.
     */
    public Player getPlayerA() {
        return playerA;
    }

    /**
     * 查询并返回 getPlayerB 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPlayerB with controlled input and output handling.
     *
     * @return 返回 Player 类型结果；Returns a result of type Player.
     */
    public Player getPlayerB() {
        return playerB;
    }

    /**
     * 校验或判断 isPaused 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of isPaused with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 查询并返回 getPausedBy 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getPausedBy with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    public String getPausedBy() {
        return pausedBy;
    }

        /**
     * 查询并返回当前对局唯一标识。
     * Returns the unique identifier of the current game.
     *
     * @return 返回对局 ID 字符串；Returns the game ID string.
     */
    public String getGameId() { return gameId; }

    /**
     * 查询并返回当前对局所在房间标识。
     * Returns the room identifier that the current game belongs to.
     *
     * @return 返回房间 ID 字符串；Returns the room ID string.
     */
    public String getRoomId() { return roomId; }

    /**
     * 校验或判断对局是否处于挂起状态。
     * Determines whether the current game is suspended.
     *
     * @return 返回挂起状态；Returns whether the game is suspended.
     */
    public boolean isSuspended() { return suspended; }

    /**
     * 查询并返回触发挂起的用户标识。
     * Returns the user identifier that triggered suspension.
     *
     * @return 返回挂起发起人 ID；Returns the suspender user ID.
     */
    public Integer getSuspendedBy() { return suspendedBy; }

    /**
     * 查询并返回当前挂起原因描述。
     * Returns the current suspension reason description.
     *
     * @return 返回挂起原因；Returns the suspension reason.
     */
    public String getSuspendedReason() { return suspendedReason; }

    /**
     * 查询并返回当前离线玩家集合。
     * Returns the current set of away/offline players.
     *
     * @return 返回离线玩家 ID 集合；Returns the set of away player IDs.
     */
    public Set<Integer> getAwayPlayers() { return awayPlayers; }

    /**
     * 更新对局挂起状态及对应上下文信息。
     * Updates suspension state together with related context.
     *
     * @param suspended 是否挂起；Whether the game is suspended.
     * @param by 挂起发起人 ID；Identifier of the suspending user.
     * @param reason 挂起原因；Reason for suspension.
     */
    public void setSuspended(boolean suspended, Integer by, String reason) {
        this.suspended = suspended;
        this.suspendedBy = by;
        this.suspendedReason = reason;
    }

    /**
     * 将指定用户加入离线玩家集合。
     * Adds the specified user into the away-player set.
     *
     * @param userId 用户 ID；User identifier.
     */
    public void addAwayPlayer(Integer userId) { awayPlayers.add(userId); }

    /**
     * 将指定用户从离线玩家集合中移除。
     * Removes the specified user from the away-player set.
     *
     * @param userId 用户 ID；User identifier.
     */
    public void removeAwayPlayer(Integer userId) { awayPlayers.remove(userId); }

    /**
     * 更新暂停状态并记录暂停来源，过程受锁保护。
     * Updates pause state and pause source under lock protection.
     *
     * @param paused 是否暂停；Whether the game is paused.
     * @param by 暂停来源标识；Identifier describing who paused the game.
     */
    public void setPaused(boolean paused, String by) {
        lock.lock();
        try {
            this.paused = paused;
            this.pausedBy = by;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 更新玩家 A 的下一步操作方向，线程安全。
     * Updates player A's next move direction in a thread-safe way.
     *
     * @param nextStepA 玩家 A 的下一步方向；Next direction of player A.
     */
    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 更新 setNextStepB 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of setNextStepB with controlled input and output handling.
     *
     * @param nextStepB 输入参数；Input parameter.
     */
    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询并返回 getG 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getG with controlled input and output handling.
     *
     * @return 返回数值结果；Returns a numeric result.
     */
    public int[][] getG() {
        return g;
    }

    /**
     * 校验或判断 check_connectivity 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of check_connectivity with controlled input and output handling.
     *
     * @param sx 输入参数；Input parameter.
     * @param sy 输入参数；Input parameter.
     * @param tx 输入参数；Input parameter.
     * @param ty 输入参数；Input parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty) return true;
        g[sx][sy] = 1;

        for (int i = 0; i < 4; i++) {
            int x = sx + dx[i], y = sy + dy[i];
            if (x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if (check_connectivity(x, y, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    /**
     * 处理 draw 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of draw with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean draw() {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                g[i][j] = 0;
            }
        }

        for (int r = 0; r < this.rows; r++) {
            g[r][0] = g[r][this.cols - 1] = 1;
        }

        for (int c = 0; c < this.cols; c++) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }

        Random random = new Random();

        for (int i = 0; i < this.inner_walls_count / 2; i++) {
            for (int j = 0; j < 1000; j++) {
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if (g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1) continue;
                if (r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) continue;

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1;
                break;
            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }

    /**
     * 创建或保存 createMap 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of createMap with controlled input and output handling.
     *
     */
    public void createMap() {
        for (int i = 0; i < 1000; i++) {
            if (draw()) break;
        }
    }

    /**
     * 查询并返回 getInput 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getInput with controlled input and output handling.
     *
     * @param player 输入参数；Input parameter.
     * @return 返回字符串结果；Returns a string result.
     */
    private String getInput(Player player) {
        Player me, you;
        if (playerA.getId().equals(player.getId())) {
            me = playerA;
            you = playerB;
        } else {
            me = playerB;
            you = playerA;
        }

        return getMapString() + "#" +
                me.getSx() + "#" +
                me.getSy() + "#(" +
                me.getStepsString() + ")#" +
                you.getSx() + "#" +
                you.getSy() + "#(" +
                you.getStepsString() + ")";
    }

    /**
     * 发送或通知 sendBotCode 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendBotCode with controlled input and output handling.
     *
     * @param player 输入参数；Input parameter.
     */
    private void sendBotCode(Player player) {
        if (player.getBotId().equals(-1)) return;
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", player.getId().toString());
        data.add("bot_code", player.getBotCode());
        data.add("input", getInput(player));
        data.add("language", player.getBotLanguage());
        WebSocketServer.restTemplate.postForObject(addBotUrl, data, String.class);
    }

    /**
     * 处理 nextStep 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of nextStep with controlled input and output handling.
     *
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean nextStep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sendBotCode(playerA);
        sendBotCode(playerB);

        int waitedMs = 0;
        while (waitedMs < STEP_TIMEOUT_MS) {
            try {
                Thread.sleep(STEP_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            lock.lock();
            try {
                if (!paused && !suspended && nextStepA != null && nextStepB != null) {
                    playerA.getSteps().add(nextStepA);
                    playerB.getSteps().add(nextStepB);
                    return true;
                }
            } finally {
                lock.unlock();
            }
            if (suspended || !paused) {
                waitedMs += STEP_POLL_INTERVAL_MS;
            }
        }

        return false;
    }

    /**
     * 校验或判断 check_valid 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of check_valid with controlled input and output handling.
     *
     * @param cellsA 集合参数；Collection parameter.
     * @param cellsB 集合参数；Collection parameter.
     * @return 返回判断结果；Returns a boolean decision result.
     */
    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.x][cell.y] == 1) return false;

        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).x == cell.x && cellsA.get(i).y == cell.y) return false;
        }

        for (int i = 0; i < n - 1; i++) {
            if (cellsB.get(i).x == cell.x && cellsB.get(i).y == cell.y) return false;
        }

        return true;
    }

    /**
     * 处理 judge 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of judge with controlled input and output handling.
     *
     */
    private void judge() {
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = check_valid(cellsA, cellsB);
        boolean validB = check_valid(cellsB, cellsA);

        if (!validA || !validB) {
            status = "finished";

            if (!validA && !validB) {
                loser = "all";
            } else if (!validA) {
                loser = "A";
            } else {
                loser = "B";
            }
        }
    }

    /**
     * 发送或通知 sendAllMessage 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendAllMessage with controlled input and output handling.
     *
     * @param message 输入参数；Input parameter.
     */
    private void sendAllMessage(String message) {
        if (WebSocketServer.users.get(playerA.getId()) != null) {
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        }
        if (WebSocketServer.users.get(playerB.getId()) != null) {
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
        }
    }

    /**
     * 发送或通知 sendMove 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendMove with controlled input and output handling.
     *
     */
    private void sendMove() {
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            sendAllMessage(resp.toJSONString());
            nextStepA = null;
            nextStepB = null;
        } finally {
            lock.unlock();
        }
        // 姣忔瀹屾垚鍚庢洿鏂?Redis 蹇収
        if (gameSnapshotService != null) {
            try { gameSnapshotService.saveStep(this); } catch (Exception e) {
                System.err.println("[Redis] saveStep failed: " + e.getMessage());
            }
        }
    }

    /**
     * 查询并返回 getMapString 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of getMapString with controlled input and output handling.
     *
     * @return 返回字符串结果；Returns a string result.
     */
    private String getMapString() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.append(g[i][j]);
            }
        }
        return res.toString();
    }

    /**
     * 更新 updateUserRating 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of updateUserRating with controlled input and output handling.
     *
     * @param player 输入参数；Input parameter.
     * @param rating 输入参数；Input parameter.
     */
    private void updateUserRating(Player player, Integer rating) {
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }

    /**
     * 创建或保存 saveToDatabase 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of saveToDatabase with controlled input and output handling.
     *
     */
    private void saveToDatabase() {
        Integer ratingA = WebSocketServer.userMapper.selectById(playerA.getId()).getRating();
        Integer ratingB = WebSocketServer.userMapper.selectById(playerB.getId()).getRating();

        if ("A".equals(loser)) {
            ratingA -= 2;
            ratingB += 5;
        } else if ("B".equals(loser)) {
            ratingA += 5;
            ratingB -= 2;
        } else {
            ratingA += 1;
            ratingB += 1;
        }

        updateUserRating(playerA, ratingA);
        updateUserRating(playerB, ratingB);

        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );

        WebSocketServer.recordMapper.insert(record);
    }

    /**
     * 发送或通知 sendResult 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of sendResult with controlled input and output handling.
     *
     */
    private void sendResult() {
        JSONObject resp = new JSONObject();
        resp.put("event", "result");
        resp.put("loser", loser);
        saveToDatabase();
        sendAllMessage(resp.toJSONString());
        WebSocketServer.clearGame(this);
        // 瀵瑰眬缁撴潫鍚庡垹闄?Redis 蹇収
        if (gameSnapshotService != null) {
            try { gameSnapshotService.deleteGame(this); } catch (Exception e) {
                System.err.println("[Redis] deleteGame failed: " + e.getMessage());
            }
        }
    }

    /**
     * 处理 run 的核心业务逻辑，并对输入输出进行约束处理。
     * Performs the core business logic of run with controlled input and output handling.
     *
     */
    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            if (nextStep()) {
                judge();
                if (status.equals("playing")) {
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
                continue;
            }

            status = "finished";
            lock.lock();
            try {
                if (nextStepA == null && nextStepB == null) {
                    loser = "all";
                } else if (nextStepA == null) {
                    loser = "A";
                } else {
                    loser = "B";
                }
            } finally {
                lock.unlock();
            }
            sendResult();
            break;
        }
    }
}
