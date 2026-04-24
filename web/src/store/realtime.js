import { defineStore } from "pinia";
import { nextTick } from "vue";
import { API_PATHS, buildWebSocketUrl } from "@/config/env";
import { apiRequest } from "@/utils/http";
import { usePkStore } from "@/store/pk";
import { useUserStore } from "@/store/user";
import defaultAvatar from "@/assets/images/default-avatar.svg";

let reconnectTimer = null;
let inviteNoticeTimer = null;
let friendRequestTimer = null;
let friendMessageTimer = null;
let manuallyClosed = false;

/**
 * 处理 clearTimer 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of clearTimer, including state updates, interaction orchestration, and error branches.
 */
const clearTimer = (timerRef) => {
  if (timerRef) clearTimeout(timerRef);
};

/**
 * 处理 syncFriendStoreRelationships 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of syncFriendStoreRelationships, including state updates, interaction orchestration, and error branches.
 */
const syncFriendStoreRelationships = () => {
  import("@/store/friend")
    .then(({ useFriendStore }) => {
      const friendStore = useFriendStore();
      const token = friendStore.getToken?.();
      if (!token) return;
      return friendStore.refreshRelationshipData();
    })
    .catch((error) => {
      console.error("refresh relationship data failed:", error);
    });
};

/**
 * 处理 pushRealtimeChatMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of pushRealtimeChatMessage, including state updates, interaction orchestration, and error branches.
 */
const pushRealtimeChatMessage = (message) => {
  import("@/store/friend")
    .then(({ useFriendStore }) => {
      useFriendStore().receivePrivateChatMessage(message);
    })
    .catch((error) => {
      console.error("push private message failed:", error);
    });
};

const normalizeInvite = (invite = {}) => ({
  id: invite.invite_id,
  senderId: invite.sender_id,
  senderUsername: invite.sender_username || "閺堫亞鐓￠悳鈺侇啀",
  senderPhoto: invite.sender_photo || defaultAvatar,
  senderBotTitle: invite.sender_bot_title || "manual",
  mapId: invite.map_id ?? null,
  mapName: invite.map_name || "随机地图",
  roomName: invite.room_name || "",
  roundSeconds: invite.round_seconds || 15,
  allowSpectator: Boolean(invite.allow_spectator ?? true),
  receiverId: invite.receiver_id,
  receiverUsername: invite.receiver_username || "閺堫亞鐓￠悳鈺侇啀",
  receiverPhoto: invite.receiver_photo || defaultAvatar,
  status: invite.status || "pending",
  createdAt: invite.created_at || "",
  expiredAt: invite.expired_at || "",
});

const normalizeFriendRequest = (request = {}) => ({
  id: request.request_id,
  senderId: request.sender_id,
  senderUsername: request.sender_username || "閺堫亞鐓￠悳鈺侇啀",
  senderPhoto: request.sender_photo || defaultAvatar,
  senderRating: request.sender_rating || 0,
    message: request.message || "sent a friend request",
    createdAt: request.created_at || "just now",
});

const normalizeFriendMessage = (message = {}) => ({
  id: message.message_id,
  senderId: message.sender_id,
  senderUsername: message.sender_username || "閺堫亞鐓￠悳鈺侇啀",
  senderPhoto: message.sender_photo || defaultAvatar,
  receiverId: message.receiver_id,
  receiverUsername: message.receiver_username || "閺堫亞鐓￠悳鈺侇啀",
  receiverPhoto: message.receiver_photo || defaultAvatar,
  content: message.content || "",
  createdAt: message.created_at || "",
});

export const useRealtimeStore = defineStore("realtime", {
  state: () => ({
    socket: null,
    connecting: false,
    initialized: false,
    pendingInvites: [],
    incomingInvite: null,
    outgoingInvite: null,
    inviteActionLoading: false,
    inviteNotice: {
      visible: false,
      type: "info",
      title: "",
      message: "",
    },
    friendRequestNotice: {
      visible: false,
      request: null,
    },
    friendMessageNotice: {
      visible: false,
      message: null,
    },
  }),
  actions: {
    /**
     * 处理 getToken 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of getToken, including state updates, interaction orchestration, and error branches.
     */
    getToken() {
      const userStore = useUserStore();
      return userStore.token || localStorage.getItem("jwt_token") || "";
    },
    /**
     * 处理 syncInviteSnapshots 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of syncInviteSnapshots, including state updates, interaction orchestration, and error branches.
     */
    syncInviteSnapshots() {
      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      this.incomingInvite = this.pendingInvites.find((invite) => invite.receiverId === currentUserId) || null;
      this.outgoingInvite = this.pendingInvites.find((invite) => invite.senderId === currentUserId) || null;
    },
    /**
     * 处理 getPendingInviteWithFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of getPendingInviteWithFriend, including state updates, interaction orchestration, and error branches.
     */
    getPendingInviteWithFriend(friendId) {
      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      return (
        this.pendingInvites.find(
          (invite) =>
            invite.senderId === currentUserId
            && invite.receiverId === friendId
            && invite.status === "pending"
        ) || null
      );
    },
    /**
     * 处理 initialize 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of initialize, including state updates, interaction orchestration, and error branches.
     */
    initialize() {
      const token = this.getToken();
      if (!token) return;
      if (this.connecting) return;
      if (this.socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(this.socket.readyState)) return;

      manuallyClosed = false;
      clearTimer(reconnectTimer);
      reconnectTimer = null;
      this.connecting = true;

      const pkStore = usePkStore();
      const socket = new WebSocket(buildWebSocketUrl(token));
      this.socket = socket;

      socket.onopen = () => {
        this.connecting = false;
        this.initialized = true;
        pkStore.updateSocket(socket);
        // 鏉╃偞甯村铏圭彌閸氬氦鍤滈崝銊ュ絺闁?sync-game閿涘瞼鏁ゆ禍搴㈡焽缁惧潡鍣告潻鐐翠划婢跺秴顕仦鈧?
        socket.send(JSON.stringify({ event: "sync-game" }));
        this.fetchPendingInvites().catch((error) => {
          console.error("fetch pending invites failed:", error);
        });
      };

      socket.onmessage = (msg) => {
        this.handleSocketMessage(msg.data);
      };

      socket.onclose = () => {
        pkStore.updateSocket(null);
        this.socket = null;
        this.connecting = false;
        if (!manuallyClosed && this.getToken()) {
          reconnectTimer = setTimeout(() => {
            reconnectTimer = null;
            this.initialize();
          }, 1500);
        }
      };

      socket.onerror = (error) => {
        console.error("realtime socket error:", error);
      };
    },
    /**
     * 处理 disconnect 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of disconnect, including state updates, interaction orchestration, and error branches.
     */
    disconnect() {
      manuallyClosed = true;
      clearTimer(reconnectTimer);
      reconnectTimer = null;
      const pkStore = usePkStore();
      pkStore.updateSocket(null);
      if (this.socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(this.socket.readyState)) {
        this.socket.close();
      }
      this.socket = null;
      this.connecting = false;
      this.initialized = false;
      this.resetNotifications();
    },
    /**
     * 处理 resetNotifications 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of resetNotifications, including state updates, interaction orchestration, and error branches.
     */
    resetNotifications() {
      clearTimer(inviteNoticeTimer);
      clearTimer(friendRequestTimer);
      clearTimer(friendMessageTimer);
      inviteNoticeTimer = null;
      friendRequestTimer = null;
      friendMessageTimer = null;
      this.pendingInvites = [];
      this.incomingInvite = null;
      this.outgoingInvite = null;
      this.inviteActionLoading = false;
      this.inviteNotice.visible = false;
      this.friendRequestNotice.visible = false;
      this.friendRequestNotice.request = null;
      this.friendMessageNotice.visible = false;
      this.friendMessageNotice.message = null;
    },
    /**
     * 处理 fetchPendingInvites 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchPendingInvites with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchPendingInvites() {
      const token = this.getToken();
      if (!token) return;
      const resp = await apiRequest(API_PATHS.friendInvitePending, { token });
      if (resp?.error_message !== "success") {
        throw new Error(resp?.error_message || "Load pending invites failed");
      }
      this.pendingInvites = (resp.invites || []).map(normalizeInvite);
      this.syncInviteSnapshots();
    },
    /**
     * 处理 showInviteNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of showInviteNotice, including state updates, interaction orchestration, and error branches.
     */
    showInviteNotice(title, message, type = "info") {
      this.inviteNotice = {
        visible: true,
        title,
        message,
        type,
      };
      clearTimer(inviteNoticeTimer);
      inviteNoticeTimer = setTimeout(() => {
        this.clearInviteNotice();
      }, 3200);
    },
    /**
     * 处理 clearInviteNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of clearInviteNotice, including state updates, interaction orchestration, and error branches.
     */
    clearInviteNotice() {
      clearTimer(inviteNoticeTimer);
      inviteNoticeTimer = null;
      this.inviteNotice.visible = false;
    },
    /**
     * 处理 showFriendRequestNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of showFriendRequestNotice, including state updates, interaction orchestration, and error branches.
     */
    showFriendRequestNotice(request) {
      this.friendRequestNotice.visible = true;
      this.friendRequestNotice.request = normalizeFriendRequest(request);
      clearTimer(friendRequestTimer);
      friendRequestTimer = setTimeout(() => {
        this.clearFriendRequestNotice();
      }, 4200);
    },
    /**
     * 处理 clearFriendRequestNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of clearFriendRequestNotice, including state updates, interaction orchestration, and error branches.
     */
    clearFriendRequestNotice() {
      clearTimer(friendRequestTimer);
      friendRequestTimer = null;
      this.friendRequestNotice.visible = false;
      this.friendRequestNotice.request = null;
    },
    /**
     * 处理 showFriendMessageNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of showFriendMessageNotice, including state updates, interaction orchestration, and error branches.
     */
    showFriendMessageNotice(message) {
      this.friendMessageNotice.visible = true;
      this.friendMessageNotice.message = normalizeFriendMessage(message);
      clearTimer(friendMessageTimer);
      friendMessageTimer = setTimeout(() => {
        this.clearFriendMessageNotice();
      }, 4200);
    },
    /**
     * 处理 clearFriendMessageNotice 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of clearFriendMessageNotice, including state updates, interaction orchestration, and error branches.
     */
    clearFriendMessageNotice() {
      clearTimer(friendMessageTimer);
      friendMessageTimer = null;
      this.friendMessageNotice.visible = false;
      this.friendMessageNotice.message = null;
    },
        /**
         * 处理 respondInvite 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
         * Handles the core frontend logic of respondInvite with async flow control, including state updates, interaction orchestration, and error branches.
         *
         * @param action 输入参数；Input parameter.
         */
        async respondInvite(action) {
      if (!this.incomingInvite || this.inviteActionLoading) return;
      const pkStore = usePkStore();
      const currentInvite = this.incomingInvite;
      this.inviteActionLoading = true;
      try {
        const resp = await apiRequest(API_PATHS.friendInviteRespond, {
          method: "POST",
          token: this.getToken(),
          data: {
            invite_id: currentInvite.id,
            action,
            receiver_bot_id: Number.parseInt(pkStore.selectedBotId, 10),
          },
        });
        if (resp?.error_message !== "success") {
          throw new Error(resp?.error_message || "Respond invite failed");
        }
        await this.fetchPendingInvites();
        syncFriendStoreRelationships();
        if (action === "accept") {
          this.showInviteNotice("Invite accepted", `Accepted invite from ${currentInvite.senderUsername}`, "success");
        } else {
          this.showInviteNotice("Invite rejected", `Rejected invite from ${currentInvite.senderUsername}`, "info");
        }
      } catch (error) {
        this.showInviteNotice("Action failed", error.message || "Invite response failed", "warn");
      } finally {
        this.inviteActionLoading = false;
      }
    },
        /**
         * 处理 cancelOutgoingInvite 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
         * Handles the core frontend logic of cancelOutgoingInvite with async flow control, including state updates, interaction orchestration, and error branches.
         *
         * @param inviteId 输入参数；Input parameter.
         */
        async cancelOutgoingInvite(inviteId = this.outgoingInvite?.id) {
      if (!inviteId || this.inviteActionLoading) return;
      const currentInvite = this.pendingInvites.find((invite) => invite.id === inviteId) || this.outgoingInvite;
      if (!currentInvite) return;
      this.inviteActionLoading = true;
      try {
        const resp = await apiRequest(API_PATHS.friendInviteRespond, {
          method: "POST",
          token: this.getToken(),
          data: {
            invite_id: inviteId,
            action: "cancel",
          },
        });
        if (resp?.error_message !== "success") {
          throw new Error(resp?.error_message || "Cancel invite failed");
        }
        await this.fetchPendingInvites();
        syncFriendStoreRelationships();
        this.showInviteNotice("Invite cancelled", `Cancelled invite to ${currentInvite.receiverUsername}`, "info");
      } catch (error) {
        this.showInviteNotice("Cancel failed", error.message || "Invite cancel failed", "warn");
      } finally {
        this.inviteActionLoading = false;
      }
    },
    /**
     * 处理 handleFriendChatMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of handleFriendChatMessage, including state updates, interaction orchestration, and error branches.
     */
    handleFriendChatMessage(message) {
      const normalized = normalizeFriendMessage(message);
      pushRealtimeChatMessage(normalized);

      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      if (normalized.senderId !== currentUserId) {
        this.showFriendMessageNotice(normalized);
      }
    },
    /**
     * 处理 handleSocketMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of handleSocketMessage, including state updates, interaction orchestration, and error branches.
     */
    async handleSocketMessage(raw) {
      const data = JSON.parse(raw);
      const pkStore = usePkStore();

      if (data.event === "start-matching") {
        this.incomingInvite = null;
        this.outgoingInvite = null;
        this.clearInviteNotice();
        pkStore.updateResultVisible(false);
        pkStore.updateLoser("none");
        // 清除上一局残留的暂离/暂停状态，防止旧状态渗入新局
        pkStore.updateAwaySuspended(false, null, "");
        pkStore.updatePaused({ isPaused: false, pausedByUserId: null });
        pkStore.updateOpponent({
          username: data.opponent_username,
          photo: data.opponent_photo,
        });
        pkStore.updateRoomId(data.room_id || "");
        pkStore.updateMatchMeta(data);
        setTimeout(() => {
          pkStore.updateStatus("playing");
        }, 200);
        pkStore.updateGame(data.game);
        syncFriendStoreRelationships();
        return;
      }

      if (data.event === "move") {
        const game = pkStore.gameObject;
        if (!game) return;
        const [snake0, snake1] = game.snakes;
        snake0.set_direction(data.a_direction);
        snake1.set_direction(data.b_direction);
        return;
      }

      if (data.event === "result") {
        const game = pkStore.gameObject;
        if (game) {
          const [snake0, snake1] = game.snakes;
          if (data.loser === "all" || data.loser === "A") snake0.status = "die";
          if (data.loser === "all" || data.loser === "B") snake1.status = "die";
        }
        pkStore.updateLoser(data.loser || "all");
        pkStore.updateResultVisible(true);
        return;
      }

      if (data.event === "game-paused") {
        pkStore.updatePaused({
          isPaused: true,
          pausedByUserId: data.paused_by,
        });
        return;
      }

      if (data.event === "game-resumed") {
        pkStore.updatePaused({ isPaused: false, pausedByUserId: null });
        return;
      }

      // 閳光偓閳光偓 閺傤厾鍤庨柌宥堢箾閺傛澘顤冩禍瀣╂ 閳光偓閳光偓
      if (data.event === "game-resync") {
        const snapshot = data.snapshot;
        const newGameId = snapshot?.game_id ?? snapshot?.gameId ?? "";
        const currentGameId = pkStore.gameId;
        // 若是不同游戏（断线后开了新局），必须强制 GameMap 卸载再重装，
        // 否则旧 GameMap 实例的墙体/地图数据不会更新，导致新局跑在旧地图上。
        const isNewGame = Boolean(newGameId && currentGameId && newGameId !== currentGameId);
        if (isNewGame && pkStore.status === "playing") {
          pkStore.updateStatus("matching"); // 触发 PlayGround/GameMap 卸载
          await nextTick();                 // 等 Vue DOM 更新（GameMap.onUnmounted 执行）
        }
        pkStore.updateGameSnapshot(snapshot);
        if (!isNewGame) {
          // 同一局内重连：直接在现有 GameMap 实例上恢复蛇身
          const gameObj = pkStore.gameObject;
          if (gameObj && typeof gameObj.restoreFromSnapshot === "function") {
            gameObj.restoreFromSnapshot();
          }
        }
        // isNewGame 时：updateGameSnapshot 已将 status 设回 "playing"，
        // Vue 会重新挂载 PlayGround/GameMap，GameMap.start() 会从新快照数据初始化。
        return;
      }

      if (data.event === "no-active-game") {
        pkStore.resetActiveGameState();
        return;
      }

      if (data.event === "player-away") {
        pkStore.markPlayerAway(data.user_id);
        return;
      }

      if (data.event === "player-back") {
        pkStore.markPlayerBack(data.user_id);
        return;
      }

      if (data.event === "game-suspended") {
        pkStore.updateAwaySuspended(true, data.suspended_by, data.reason);
        return;
      }

      if (data.event === "game-resumed-from-away") {
        pkStore.updateAwaySuspended(false, null, "");
        return;
      }

      if (data.event === "friend-request-received") {
        this.showFriendRequestNotice(data.request || {});
        syncFriendStoreRelationships();
        return;
      }

            if (data.event === "friend-request-handled") {
        if (data.status === "accepted") {
          this.showInviteNotice("Friend request accepted", "Your friend request was accepted", "success");
        }
        syncFriendStoreRelationships();
        return;
      }

            if (data.event === "friend-invite-received") {
        const invite = normalizeInvite(data.invite || {});
        this.showInviteNotice("Challenge received", `${invite.senderUsername} invited you to a match`, "success");
        this.fetchPendingInvites().catch((error) => {
          console.error("refresh pending invites failed:", error);
        });
        syncFriendStoreRelationships();
        return;
      }

      if (data.event === "friend-invite-updated") {
        this.handleInviteUpdated(data);
        this.fetchPendingInvites().catch((error) => {
          console.error("refresh pending invites failed:", error);
        });
        syncFriendStoreRelationships();
        return;
      }

      if (data.event === "friend-chat-message") {
        this.handleFriendChatMessage(data.message || {});
      }
    },
    /**
     * 处理 handleInviteUpdated 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of handleInviteUpdated, including state updates, interaction orchestration, and error branches.
     */
        handleInviteUpdated(data) {
      const status = data.status || "updated";
      if (this.incomingInvite && this.incomingInvite.id === data.invite_id) {
        const senderName = this.incomingInvite.senderUsername;
        if (status === "cancelled") {
          this.showInviteNotice("Invite cancelled", `${senderName} cancelled this invite`, "info");
        } else if (status === "expired") {
          this.showInviteNotice("Invite expired", `${senderName}'s invite expired`, "warn");
        }
        return;
      }
      if (this.outgoingInvite && this.outgoingInvite.id === data.invite_id) {
        const receiverName = this.outgoingInvite.receiverUsername;
        if (status === "accepted") {
          this.showInviteNotice("Invite accepted", `${receiverName} accepted your invite`, "success");
        } else if (status === "rejected") {
          this.showInviteNotice("Invite rejected", `${receiverName} rejected your invite`, "warn");
        } else if (status === "cancelled") {
          this.showInviteNotice("Invite cancelled", `You cancelled invite to ${receiverName}`, "info");
        } else if (status === "expired") {
          this.showInviteNotice("Invite expired", `Invite to ${receiverName} expired`, "warn");
        }
      }
    },
  },
});


