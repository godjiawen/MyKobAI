import { defineStore } from "pinia";

export const usePkStore = defineStore("pk", {
  state: () => ({
    // 状态字段：用于区分“匹配中”和“对局中”两种阶段
    status: "matching",
    // 当前对局的网络连接实例
    socket: null,
    opponent_username: "",
    opponent_photo: "",
    gamemap: null,
    a_id: 0,
    a_sx: 0,
    a_sy: 0,
    b_id: 0,
    b_sx: 0,
    b_sy: 0,
    // 地图对象运行时实例（包含两条蛇、墙体等）
    gameObject: null,
    loser: "none",
    resultVisible: false,
    // 前后端共享的聊天室标识（双方 id 排序拼接）
    roomId: "",
    // 暂停状态由服务端广播同步
    isPaused: false,
    pausedByUserId: null,
    selectedBotId: "-1",
    // 断线重连字段
    gameId: "",
    aSteps: "",
    bSteps: "",
    isAwaySuspended: false,
    awayUserIds: [],
    suspendedByUserId: null,
    suspendedReason: "",
    lastSnapshotAt: 0,
  }),
  actions: {
    normalizeUserId(value) {
      if (value === null || value === undefined || value === "") return null;
      const parsed = Number.parseInt(value, 10);
      return Number.isNaN(parsed) ? null : parsed;
    },
    resolvePausedByUserId(value, aId, bId) {
      if (value === "A") return this.normalizeUserId(aId);
      if (value === "B") return this.normalizeUserId(bId);
      return this.normalizeUserId(value);
    },
    /**
     * Handles updateSocket.
     * ??updateSocket?
     */
    updateSocket(socket) {
      this.socket = socket;
    },
    /**
     * Handles updateOpponent.
     * ??updateOpponent?
     */
    updateOpponent(opponent) {
      this.opponent_username = opponent.username;
      this.opponent_photo = opponent.photo;
    },
    /**
     * Handles updateStatus.
     * ??updateStatus?
     */
    updateStatus(status) {
      this.status = status;
    },
    /**
     * Handles updateGame.
     * ??updateGame?
     */
    updateGame(game) {
      // 将后端下发的对局快照拆分并写入状态。
      this.gamemap = game.map;
      this.a_id = game.a_id;
      this.a_sx = game.a_sx;
      this.a_sy = game.a_sy;
      this.b_id = game.b_id;
      this.b_sx = game.b_sx;
      this.b_sy = game.b_sy;
    },
    /**
     * Handles updateGameObject.
     * ??updateGameObject?
     */
    updateGameObject(gameObject) {
      this.gameObject = gameObject;
    },
    /**
     * Handles updateLoser.
     * ??updateLoser?
     */
    updateLoser(loser) {
      this.loser = loser;
    },
    /**
     * Handles updateResultVisible.
     * ??updateResultVisible?
     */
    updateResultVisible(visible) {
      this.resultVisible = visible;
    },
    /**
     * Handles updateRoomId.
     * ??updateRoomId?
     */
    updateRoomId(roomId) {
      this.roomId = roomId;
    },
    /**
     * Handles updatePaused.
     * ??updatePaused?
     */
    updatePaused({ isPaused, pausedByUserId }) {
      this.isPaused = isPaused;
      this.pausedByUserId = this.normalizeUserId(pausedByUserId);
    },
    /**
     * Handles updateSelectedBot.
     * ??updateSelectedBot?
     */
    updateSelectedBot(botId) {
      this.selectedBotId = String(botId ?? "-1");
    },
    /**
     * 断线重连：从 Redis 快照更新全部状态
     */
    updateGameSnapshot(snapshot) {
      if (!snapshot) return;
      this.gameId = snapshot.game_id ?? snapshot.gameId ?? "";
      this.gamemap = snapshot.map;
      const aId = snapshot.a_id ?? snapshot.aId ?? 0;
      const bId = snapshot.b_id ?? snapshot.bId ?? 0;
      this.a_id = aId;
      this.b_id = bId;
      this.a_sx = snapshot.a_sx ?? snapshot.aSx ?? 0;
      this.a_sy = snapshot.a_sy ?? snapshot.aSy ?? 0;
      this.b_sx = snapshot.b_sx ?? snapshot.bSx ?? 0;
      this.b_sy = snapshot.b_sy ?? snapshot.bSy ?? 0;
      this.aSteps = snapshot.a_steps ?? snapshot.aSteps ?? "";
      this.bSteps = snapshot.b_steps ?? snapshot.bSteps ?? "";
      this.roomId = snapshot.room_id ?? snapshot.roomId ?? "";
      this.isPaused = Boolean(snapshot.paused);
      this.pausedByUserId = this.resolvePausedByUserId(
        snapshot.paused_by ?? snapshot.pausedBy ?? null,
        aId,
        bId
      );
      this.isAwaySuspended = Boolean(snapshot.suspended);
      this.awayUserIds = snapshot.away_user_ids ?? snapshot.awayUserIds ?? [];
      this.suspendedByUserId = this.normalizeUserId(snapshot.suspended_by ?? snapshot.suspendedBy ?? null);
      this.suspendedReason = snapshot.suspended_reason ?? snapshot.suspendedReason ?? "";
      this.lastSnapshotAt = snapshot.updated_at ?? snapshot.updatedAt ?? Date.now();
      // 若快照状态是 playing，切换到对局页
      const status = snapshot.status;
      if (status && status !== "finished") {
        this.status = "playing";
      }
    },
    resetActiveGameState() {
      this.status = "matching";
      this.opponent_username = "";
      this.opponent_photo = "";
      this.gamemap = null;
      this.a_id = 0;
      this.a_sx = 0;
      this.a_sy = 0;
      this.b_id = 0;
      this.b_sx = 0;
      this.b_sy = 0;
      this.loser = "none";
      this.resultVisible = false;
      this.roomId = "";
      this.isPaused = false;
      this.pausedByUserId = null;
      this.gameId = "";
      this.aSteps = "";
      this.bSteps = "";
      this.isAwaySuspended = false;
      this.awayUserIds = [];
      this.suspendedByUserId = null;
      this.suspendedReason = "";
      this.lastSnapshotAt = 0;
    },
    markPlayerAway(userId) {
      if (!this.awayUserIds.includes(userId)) {
        this.awayUserIds = [...this.awayUserIds, userId];
      }
      this.isAwaySuspended = true;
    },
    markPlayerBack(userId) {
      this.awayUserIds = this.awayUserIds.filter((id) => id !== userId);
    },
    updateAwaySuspended(suspended, suspendedBy, reason) {
      this.isAwaySuspended = suspended;
      this.suspendedByUserId = this.normalizeUserId(suspendedBy);
      this.suspendedReason = reason || "";
    },
  },
});

