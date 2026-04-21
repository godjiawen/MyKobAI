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

/**
 * Handles clearTimer.
 * ??clearTimer?
 */
const clearTimer = (timerRef) => {
  if (timerRef) clearTimeout(timerRef);
};

/**
 * Handles syncFriendStoreRelationships.
 * ??syncFriendStoreRelationships?
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
 * Handles pushRealtimeChatMessage.
 * ??pushRealtimeChatMessage?
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
     * Handles getToken.
     * ??getToken?
     */
    getToken() {
      const userStore = useUserStore();
      return userStore.token || localStorage.getItem("jwt_token") || "";
    },
    /**
     * Handles syncInviteSnapshots.
     * ??syncInviteSnapshots?
     */
    syncInviteSnapshots() {
      const userStore = useUserStore();
      const currentUserId = Number.parseInt(userStore.id, 10);
      this.incomingInvite = this.pendingInvites.find((invite) => invite.receiverId === currentUserId) || null;
      this.outgoingInvite = this.pendingInvites.find((invite) => invite.senderId === currentUserId) || null;
    },
    /**
     * Handles getPendingInviteWithFriend.
     * ??getPendingInviteWithFriend?
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
     * Handles initialize.
     * ??initialize?
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
     * Handles disconnect.
     * ??disconnect?
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
     * Handles resetNotifications.
     * ??resetNotifications?
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
     * Handles showInviteNotice.
     * ??showInviteNotice?
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
     * Handles clearInviteNotice.
     * ??clearInviteNotice?
     */
    clearInviteNotice() {
      clearTimer(inviteNoticeTimer);
      inviteNoticeTimer = null;
      this.inviteNotice.visible = false;
    },
    /**
     * Handles showFriendRequestNotice.
     * ??showFriendRequestNotice?
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
     * Handles clearFriendRequestNotice.
     * ??clearFriendRequestNotice?
     */
    clearFriendRequestNotice() {
      clearTimer(friendRequestTimer);
      friendRequestTimer = null;
      this.friendRequestNotice.visible = false;
      this.friendRequestNotice.request = null;
    },
    /**
     * Handles showFriendMessageNotice.
     * ??showFriendMessageNotice?
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
     * Handles clearFriendMessageNotice.
     * ??clearFriendMessageNotice?
     */
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
     * Handles handleFriendChatMessage.
     * ??handleFriendChatMessage?
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
     * Handles handleSocketMessage.
     * ??handleSocketMessage?
     */
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

      // 閳光偓閳光偓 閺傤厾鍤庨柌宥堢箾閺傛澘顤冩禍瀣╂ 閳光偓閳光偓
      if (data.event === "game-resync") {
        pkStore.updateGameSnapshot(data.snapshot);
        // 婵″倹鐏?GameMap 瀹歌尙绮￠崚娑樼紦閿涘牏鏁ら幋鐤箲閸ョ偤銆夐棃銏℃閿涘绱濋棁鈧憰渚€鍣搁弬鐗堜划婢跺秷娉ч煬?
        const gameObj = pkStore.gameObject;
        if (gameObj && typeof gameObj.restoreFromSnapshot === "function") {
          gameObj.restoreFromSnapshot();
        }
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
     * Handles handleInviteUpdated.
     * ??handleInviteUpdated?
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


