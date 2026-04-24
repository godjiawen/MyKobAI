import { defineStore } from "pinia";
import defaultAvatar from "@/assets/images/default-avatar.svg";

export const usePkStore = defineStore("pk", {
  state: () => ({
    // 状态字段：用于区分“匹配中”和“对局中”两种阶段
    status: "matching",
    // 当前对局的网络连接实例
    socket: null,
    opponent_username: "匹配对手",
    opponent_photo: defaultAvatar,
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
    matchType: "ranked",
    mapId: null,
    mapName: "随机地图",
    allowSpectator: true,
    roundSeconds: 15,
    spectatorCount: 0,
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
    /**
     * 处理 normalizeUserId 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of normalizeUserId with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param value 输入参数；Input parameter.
     */
    normalizeUserId(value) {
      if (value === null || value === undefined || value === "") return null;
      const parsed = Number.parseInt(value, 10);
      return Number.isNaN(parsed) ? null : parsed;
    },
    /**
     * 处理 resolvePausedByUserId 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of resolvePausedByUserId with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param value 输入参数；Input parameter.
     * @param aId 输入参数；Input parameter.
     * @param bId 输入参数；Input parameter.
     */
    resolvePausedByUserId(value, aId, bId) {
      if (value === "A") return this.normalizeUserId(aId);
      if (value === "B") return this.normalizeUserId(bId);
      return this.normalizeUserId(value);
    },
    /**
     * 处理 updateSocket 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateSocket, including state updates, interaction orchestration, and error branches.
     */
    updateSocket(socket) {
      this.socket = socket;
    },
    /**
     * 处理 updateOpponent 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateOpponent, including state updates, interaction orchestration, and error branches.
     */
    updateOpponent(opponent) {
      this.opponent_username = opponent.username;
      this.opponent_photo = opponent.photo;
    },
    /**
     * 处理 updateStatus 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateStatus, including state updates, interaction orchestration, and error branches.
     */
    updateStatus(status) {
      this.status = status;
    },
    /**
     * 处理 updateGame 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateGame, including state updates, interaction orchestration, and error branches.
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
     * 处理 updateGameObject 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateGameObject, including state updates, interaction orchestration, and error branches.
     */
    updateGameObject(gameObject) {
      this.gameObject = gameObject;
    },
    /**
     * 处理 updateLoser 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateLoser, including state updates, interaction orchestration, and error branches.
     */
    updateLoser(loser) {
      this.loser = loser;
    },
    /**
     * 处理 updateResultVisible 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateResultVisible, including state updates, interaction orchestration, and error branches.
     */
    updateResultVisible(visible) {
      this.resultVisible = visible;
    },
    /**
     * 处理 updateRoomId 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateRoomId, including state updates, interaction orchestration, and error branches.
     */
    updateRoomId(roomId) {
      this.roomId = roomId;
    },
    /**
     * 处理 updatePaused 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updatePaused, including state updates, interaction orchestration, and error branches.
     */
    updatePaused({ isPaused, pausedByUserId }) {
      this.isPaused = isPaused;
      this.pausedByUserId = this.normalizeUserId(pausedByUserId);
    },
    /**
     * 处理 updateSelectedBot 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateSelectedBot, including state updates, interaction orchestration, and error branches.
     */
    updateSelectedBot(botId) {
      this.selectedBotId = String(botId ?? "-1");
    },
    updateMatchMeta(meta = {}) {
      this.matchType = meta.match_type ?? meta.matchType ?? "ranked";
      this.mapId = meta.map_id ?? meta.mapId ?? null;
      this.mapName = meta.map_name ?? meta.mapName ?? "随机地图";
      this.allowSpectator = Boolean(meta.allow_spectator ?? meta.allowSpectator ?? true);
      this.roundSeconds = Number(meta.round_seconds ?? meta.roundSeconds ?? 15);
      this.spectatorCount = Number(meta.spectator_count ?? meta.spectatorCount ?? 0);
    },
    updateSpectatorCount(count) {
      this.spectatorCount = Number(count) || 0;
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
      this.updateMatchMeta(snapshot);
      // 若快照状态是 playing，切换到对局页
      const status = snapshot.status;
      if (status && status !== "finished") {
        this.status = "playing";
      }
    },
    /**
     * 处理 resetActiveGameState 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of resetActiveGameState with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    resetActiveGameState() {
      this.status = "matching";
      this.opponent_username = "匹配对手";
      this.opponent_photo = defaultAvatar;
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
      this.matchType = "ranked";
      this.mapId = null;
      this.mapName = "随机地图";
      this.allowSpectator = true;
      this.roundSeconds = 15;
      this.spectatorCount = 0;
      this.gameId = "";
      this.aSteps = "";
      this.bSteps = "";
      this.isAwaySuspended = false;
      this.awayUserIds = [];
      this.suspendedByUserId = null;
      this.suspendedReason = "";
      this.lastSnapshotAt = 0;
    },
    /**
     * 处理 markPlayerAway 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of markPlayerAway with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param userId 输入参数；Input parameter.
     */
    markPlayerAway(userId) {
      if (!this.awayUserIds.includes(userId)) {
        this.awayUserIds = [...this.awayUserIds, userId];
      }
      this.isAwaySuspended = true;
    },
    /**
     * 处理 markPlayerBack 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of markPlayerBack with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param userId 输入参数；Input parameter.
     */
    markPlayerBack(userId) {
      this.awayUserIds = this.awayUserIds.filter((id) => id !== userId);
    },
    /**
     * 处理 updateAwaySuspended 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateAwaySuspended with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param suspended 输入参数；Input parameter.
     * @param suspendedBy 输入参数；Input parameter.
     * @param reason 输入参数；Input parameter.
     */
    updateAwaySuspended(suspended, suspendedBy, reason) {
      this.isAwaySuspended = suspended;
      this.suspendedByUserId = this.normalizeUserId(suspendedBy);
      this.suspendedReason = reason || "";
      // 恢复时同步清空离场列表，防止旧状态渗入新局
      if (!suspended) {
        this.awayUserIds = [];
      }
    },
  },
});

