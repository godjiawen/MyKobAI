import { defineStore } from "pinia";
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

const clearTimer = (timerRef) => {
  if (timerRef) clearTimeout(timerRef);
};

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
  senderUsername: invite.sender_username || "未知玩家",
  senderPhoto: invite.sender_photo || defaultAvatar,
  senderBotTitle: invite.sender_bot_title || "manual",
  receiverId: invite.receiver_id,
  receiverUsername: invite.receiver_username || "未知玩家",
  receiverPhoto: invite.receiver_photo || defaultAvatar,
  status: invite.status || "pending",
  createdAt: invite.created_at || "",
  expiredAt: invite.expired_at || "",
});

const normalizeFriendRequest = (request = {}) => ({
  id: request.request_id,
  senderId: request.sender_id,
  senderUsername: request.sender_username || "未知玩家",
  senderPhoto: request.sender_photo || defaultAvatar,
  senderRating: request.sender_rating || 0,
  message: request.message || "向你发送了好友申请。",
  createdAt: request.created_at || "刚刚",
});

const normalizeFriendMessage = (message = {}) => ({
  id: message.message_id,
  senderId: message.sender_id,
  senderUsername: message.sender_username || "未知玩家",
  senderPhoto: message.sender_photo || defaultAvatar,
  receiverId: message.receiver_id,
  receiverUsername: message.receiver_username || "未知玩家",
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
    getToken() {
      const userStore = useUserStore();
      return userStore.token || localStorage.getItem("jwt_token") || "";
    },
    syncInviteSnapshots() {
      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      this.incomingInvite = this.pendingInvites.find((invite) => invite.receiverId === currentUserId) || null;
      this.outgoingInvite = this.pendingInvites.find((invite) => invite.senderId === currentUserId) || null;
    },
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
    clearInviteNotice() {
      clearTimer(inviteNoticeTimer);
      inviteNoticeTimer = null;
      this.inviteNotice.visible = false;
    },
    showFriendRequestNotice(request) {
      this.friendRequestNotice.visible = true;
      this.friendRequestNotice.request = normalizeFriendRequest(request);
      clearTimer(friendRequestTimer);
      friendRequestTimer = setTimeout(() => {
        this.clearFriendRequestNotice();
      }, 4200);
    },
    clearFriendRequestNotice() {
      clearTimer(friendRequestTimer);
      friendRequestTimer = null;
      this.friendRequestNotice.visible = false;
      this.friendRequestNotice.request = null;
    },
    showFriendMessageNotice(message) {
      this.friendMessageNotice.visible = true;
      this.friendMessageNotice.message = normalizeFriendMessage(message);
      clearTimer(friendMessageTimer);
      friendMessageTimer = setTimeout(() => {
        this.clearFriendMessageNotice();
      }, 4200);
    },
    clearFriendMessageNotice() {
      clearTimer(friendMessageTimer);
      friendMessageTimer = null;
      this.friendMessageNotice.visible = false;
      this.friendMessageNotice.message = null;
    },
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
          this.showInviteNotice("邀战已接受", `已接受 ${currentInvite.senderUsername} 的邀战。`, "success");
        } else {
          this.showInviteNotice("已拒绝邀战", `你已拒绝 ${currentInvite.senderUsername} 的邀战。`, "info");
        }
      } catch (error) {
        this.showInviteNotice("处理失败", error.message || "邀战响应失败。", "warn");
      } finally {
        this.inviteActionLoading = false;
      }
    },
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
        this.showInviteNotice("邀战已撤回", `已撤回发给 ${currentInvite.receiverUsername} 的邀战。`, "info");
      } catch (error) {
        this.showInviteNotice("撤回失败", error.message || "邀战撤回失败。", "warn");
      } finally {
        this.inviteActionLoading = false;
      }
    },
    handleFriendChatMessage(message) {
      const normalized = normalizeFriendMessage(message);
      pushRealtimeChatMessage(normalized);

      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      if (normalized.senderId !== currentUserId) {
        this.showFriendMessageNotice(normalized);
      }
    },
    handleSocketMessage(raw) {
      const data = JSON.parse(raw);
      const pkStore = usePkStore();

      if (data.event === "start-matching") {
        this.incomingInvite = null;
        this.outgoingInvite = null;
        this.clearInviteNotice();
        pkStore.updateResultVisible(false);
        pkStore.updateLoser("none");
        pkStore.updateOpponent({
          username: data.opponent_username,
          photo: data.opponent_photo,
        });
        pkStore.updateRoomId(data.room_id || "");
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

      if (data.event === "friend-request-received") {
        this.showFriendRequestNotice(data.request || {});
        syncFriendStoreRelationships();
        return;
      }

      if (data.event === "friend-request-handled") {
        if (data.status === "accepted") {
          this.showInviteNotice("好友申请已通过", "你发出的好友申请已经被接受。", "success");
        }
        syncFriendStoreRelationships();
        return;
      }

      if (data.event === "friend-invite-received") {
        const invite = normalizeInvite(data.invite || {});
        this.showInviteNotice("收到好友邀战", `${invite.senderUsername} 向你发起了对战邀请。`, "success");
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
    handleInviteUpdated(data) {
      const status = data.status || "updated";

      if (this.incomingInvite && this.incomingInvite.id === data.invite_id) {
        const senderName = this.incomingInvite.senderUsername;
        if (status === "cancelled") {
          this.showInviteNotice("邀战已取消", `${senderName} 取消了这次邀战。`, "info");
        } else if (status === "expired") {
          this.showInviteNotice("邀战已过期", `${senderName} 的邀战已失效。`, "warn");
        }
        return;
      }

      if (this.outgoingInvite && this.outgoingInvite.id === data.invite_id) {
        const receiverName = this.outgoingInvite.receiverUsername;
        if (status === "accepted") {
          this.showInviteNotice("对方已接受", `${receiverName} 已接受你的邀战。`, "success");
        } else if (status === "rejected") {
          this.showInviteNotice("对方已拒绝", `${receiverName} 拒绝了这次邀战。`, "warn");
        } else if (status === "cancelled") {
          this.showInviteNotice("邀战已撤回", `你已撤回发给 ${receiverName} 的邀战。`, "info");
        } else if (status === "expired") {
          this.showInviteNotice("邀战已过期", `发给 ${receiverName} 的邀战已过期。`, "warn");
        }
      }
    },
  },
});
