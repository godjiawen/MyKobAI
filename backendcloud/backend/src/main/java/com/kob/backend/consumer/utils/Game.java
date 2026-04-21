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
     * Constructor: initializes game with map dimensions, wall count, player IDs and bot settings.
     * 鏋勯€犲嚱鏁帮細浣跨敤鍦板浘灏哄銆佸鏁伴噺銆佺帺瀹禝D鍜屾満鍣ㄤ汉閰嶇疆鍒濆鍖栨父鎴忋€?
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
     * Returns player A.
     * 杩斿洖鐜╁A銆?
     */
    public Player getPlayerA() {
        return playerA;
    }

    /**
     * Returns player B.
     * 杩斿洖鐜╁B銆?
     */
    public Player getPlayerB() {
        return playerB;
    }

    /**
     * Returns whether the game is currently paused.
     * 杩斿洖娓告垙褰撳墠鏄惁澶勪簬鏆傚仠鐘舵€併€?
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Returns the identifier of the player who paused the game.
     * 杩斿洖鏆傚仠娓告垙鐨勭帺瀹舵爣璇嗐€?
     */
    public String getPausedBy() {
        return pausedBy;
    }

    public String getGameId() { return gameId; }
    public String getRoomId() { return roomId; }
    public boolean isSuspended() { return suspended; }
    public Integer getSuspendedBy() { return suspendedBy; }
    public String getSuspendedReason() { return suspendedReason; }
    public Set<Integer> getAwayPlayers() { return awayPlayers; }

    public void setSuspended(boolean suspended, Integer by, String reason) {
        this.suspended = suspended;
        this.suspendedBy = by;
        this.suspendedReason = reason;
    }

    public void addAwayPlayer(Integer userId) { awayPlayers.add(userId); }
    public void removeAwayPlayer(Integer userId) { awayPlayers.remove(userId); }

    /**
     * Sets the pause state and records who triggered the pause, thread-safe.
     * 绾跨▼瀹夊叏鍦拌缃父鎴忔殏鍋滅姸鎬侊紝骞惰褰曡Е鍙戞柟銆?
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
     * Sets the next move direction for player A, thread-safe.
     * 绾跨▼瀹夊叏鍦拌缃帺瀹禔鐨勪笅涓€姝ョЩ鍔ㄦ柟鍚戙€?
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
     * Sets the next move direction for player B, thread-safe.
     * 绾跨▼瀹夊叏鍦拌缃帺瀹禕鐨勪笅涓€姝ョЩ鍔ㄦ柟鍚戙€?
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
     * Returns the game map grid.
     * 杩斿洖娓告垙鍦板浘缃戞牸鏁扮粍銆?
     */
    public int[][] getG() {
        return g;
    }

    /**
     * Checks connectivity between two cells using DFS, returns true if reachable.
     * 浣跨敤娣卞害浼樺厛鎼滅储妫€鏌ヤ袱鏍间箣闂寸殑杩為€氭€э紝鍙揪鍒欒繑鍥瀟rue銆?
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
     * Randomly generates walls on the map with symmetry and validates connectivity.
     * 闅忔満瀵圭О鐢熸垚鍦板浘鍐呴儴澧欎綋骞堕獙璇佽繛閫氭€с€?
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
     * Creates a valid map by repeating random generation until connectivity is satisfied.
     * 閲嶅闅忔満鐢熸垚鍦板浘鐩村埌婊¤冻杩為€氭€ц姹傘€?
     */
    public void createMap() {
        for (int i = 0; i < 1000; i++) {
            if (draw()) break;
        }
    }

    /**
     * Builds the input string for the bot: map, self position/steps, opponent position/steps.
     * 鏋勫缓鏈哄櫒浜鸿緭鍏ュ瓧绗︿覆锛氬湴鍥俱€佽嚜韬綅缃?姝ラ銆佸鎵嬩綅缃?姝ラ銆?
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
     * Sends bot code and input to the bot running system via HTTP.
     * 閫氳繃HTTP灏嗘満鍣ㄤ汉浠ｇ爜鍜岃緭鍏ュ彂閫佸埌鏈哄櫒浜鸿繍琛岀郴缁熴€?
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
     * Waits for both players to submit their next step within the timeout; returns true if both submitted.
     * 绛夊緟鍙屾柟鐜╁鍦ㄨ秴鏃舵椂闄愬唴鎻愪氦涓嬩竴姝ワ紱鍙屾柟鍧囨彁浜ゅ垯杩斿洖true銆?
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
     * Checks if a player's latest move is valid (not hitting walls or snake bodies).
     * 妫€鏌ョ帺瀹舵渶鏂扮Щ鍔ㄦ槸鍚﹀悎娉曪紙鏈挒澧欐垨铔囪韩锛夈€?
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
     * Judges the current game state and determines the loser if either player made an invalid move.
     * 鍒ゆ柇褰撳墠娓告垙鐘舵€侊紝鑻ヤ换鎰忕帺瀹剁Щ鍔ㄩ潪娉曞垯纭畾澶辫触鏂广€?
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
     * Sends a message to both players via WebSocket.
     * 閫氳繃WebSocket鍚戜袱浣嶇帺瀹跺彂閫佹秷鎭€?
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
     * Broadcasts both players' move directions to all participants and clears the next steps.
     * 骞挎挱鍙屾柟绉诲姩鏂瑰悜缁欐墍鏈夊弬涓庤€呭苟娓呯┖涓嬩竴姝ョ紦瀛樸€?
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
     * Serializes the map grid to a string for transmission.
     * 灏嗗湴鍥剧綉鏍煎簭鍒楀寲涓哄瓧绗︿覆浠ヤ究浼犺緭銆?
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
     * Updates a player's rating in the database.
     * 鍦ㄦ暟鎹簱涓洿鏂扮帺瀹剁殑璇勫垎銆?
     */
    private void updateUserRating(Player player, Integer rating) {
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }

    /**
     * Adjusts ratings for both players based on outcome and saves the game record to the database.
     * 鏍规嵁瀵瑰眬缁撴灉璋冩暣鍙屾柟璇勫垎骞跺皢瀵瑰眬璁板綍瀛樺叆鏁版嵁搴撱€?
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
     * Saves the game result to the database and notifies all players.
     * 灏嗗灞€缁撴灉淇濆瓨鍒版暟鎹簱骞堕€氱煡鎵€鏈夌帺瀹躲€?
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
     * Main game loop: processes steps, judges validity, sends moves or result until game ends.
     * 娓告垙涓诲惊鐜細澶勭悊姝ラ銆佸垽鏂悎娉曟€с€佸彂閫佺Щ鍔ㄦ垨缁撴灉鐩村埌娓告垙缁撴潫銆?
     */
    @Override
    /**
     * Handles run.
     * ??run?
     */
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


