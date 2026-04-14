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
    // 前后端共享的聊天房间标识（双方 id 排序拼接）
    roomId: "",
    // 暂离状态由服务端广播同步
    isPaused: false,
    pausedByUserId: null,
  }),
  actions: {
    updateSocket(socket) {
      this.socket = socket;
    },
    updateOpponent(opponent) {
      this.opponent_username = opponent.username;
      this.opponent_photo = opponent.photo;
    },
    updateStatus(status) {
      this.status = status;
    },
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
    updateGameObject(gameObject) {
      this.gameObject = gameObject;
    },
    updateLoser(loser) {
      this.loser = loser;
    },
    updateResultVisible(visible) {
      this.resultVisible = visible;
    },
    updateRoomId(roomId) {
      this.roomId = roomId;
    },
    updatePaused({ isPaused, pausedByUserId }) {
      this.isPaused = isPaused;
      this.pausedByUserId = pausedByUserId ?? null;
    },
  },
});
