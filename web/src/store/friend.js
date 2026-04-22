import { defineStore } from "pinia";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import { useUserStore } from "@/store/user";
import { useRealtimeStore } from "@/store/realtime";
import { usePkStore } from "@/store/pk";
import defaultAvatar from "@/assets/images/default-avatar.svg";

let feedbackTimer = null;
let discoverSearchTimer = null;
const DISCOVER_DISPLAY_LIMIT = 9;
const CHAT_HISTORY_PAGE_SIZE = 60;

const statusOrder = { online: 0, busy: 1, offline: 2 };
const relationOrder = { incoming: 0, pending: 1, none: 2, friend: 3, self: 4 };
const statusLabelMap = {
  online: "在线",
  busy: "对局中",
  offline: "离线",
};
const requestStatusLabelMap = {
  pending: "待处理",
  accepted: "已通过",
  ignored: "已忽略",
  cancelled: "已撤回",
  expired: "已过期",
};

const normalizeKeyword = (value) => (value || "").trim().toLowerCase();

/**
 * 处理 matchesKeyword 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of matchesKeyword, including state updates, interaction orchestration, and error branches.
 */
const matchesKeyword = (keyword, fields) => {
  if (!keyword) return true;
  return fields.some((field) => String(field || "").toLowerCase().includes(keyword));
};

/**
 * 处理 mapOnlineStatus 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of mapOnlineStatus, including state updates, interaction orchestration, and error branches.
 */
const mapOnlineStatus = (status) => {
  if (status === "in_game" || status === "busy") return "busy";
  if (status === "online") return "online";
  return "offline";
};

const buildStatusLabel = (status) => statusLabelMap[status] || "离线";

/**
 * 处理 isToday 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of isToday, including state updates, interaction orchestration, and error branches.
 */
const isToday = (value) => {
  if (!value) return false;
  const target = new Date(value.replace(/-/g, "/"));
  if (Number.isNaN(target.getTime())) return false;
  const now = new Date();
  return now.getFullYear() === target.getFullYear()
    && now.getMonth() === target.getMonth()
    && now.getDate() === target.getDate();
};

const safeDateLabel = (value, fallback = "暂无记录") => value || fallback;

const normalizeBotOption = (bot = {}) => ({
  id: String(bot.id ?? "-1"),
  title: bot.title || "未命名 Bot",
  description: bot.description || "",
});

/**
 * 处理 buildFriendTags 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of buildFriendTags, including state updates, interaction orchestration, and error branches.
 */
const buildFriendTags = (item, uiStatus) => {
  const tags = [];
  if (item.is_favorite) tags.push("常用好友");
  if (item.match_count > 0) tags.push(`共同对局 ${item.match_count} 场`);
  if (item.last_match_at) tags.push("近期互动");
  if (uiStatus === "busy") tags.push("对局中");
  if (uiStatus === "online") tags.push("在线可邀战");
  return tags.slice(0, 3).length ? tags.slice(0, 3) : ["等待建立互动"]; 
};

/**
 * 处理 mapFriendItem 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of mapFriendItem, including state updates, interaction orchestration, and error branches.
 */
const mapFriendItem = (item) => {
  const status = mapOnlineStatus(item.online_status);
  const tags = buildFriendTags(item, status);
  return {
    id: item.friend_id,
    name: item.friend_username,
    photo: item.friend_photo || defaultAvatar,
    status,
    statusLabel: buildStatusLabel(status),
    signature: item.last_match_at
      ? `最近一次共同对局：${item.last_match_at}`
      : "还没有共同对局，来一场吧。",
    note: item.match_count > 0
      ? `已共同对局 ${item.match_count} 场。`
      : "刚成为好友，适合先热身一局。",
    rating: item.friend_rating,
    commonMatches: item.match_count || 0,
    winRate: item.match_count > 0 ? `已记录 ${item.match_count} 场` : "待建立",
    region: "暂未公开",
    lastSeen: safeDateLabel(item.last_active_at, "暂无活跃记录"),
    favorite: Boolean(item.is_favorite),
    recentlyPlayed: Boolean(item.last_match_at),
    newToday: isToday(item.created_at),
    tags,
    activity: [
      { label: "成为好友", value: safeDateLabel(item.created_at) },
      { label: "最近对局", value: safeDateLabel(item.last_match_at) },
      { label: "互动总数", value: `${item.match_count || 0} 场` },
    ],
  };
};

/**
 * 处理 mapRequestItem 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of mapRequestItem, including state updates, interaction orchestration, and error branches.
 */
const mapRequestItem = (item, perspective) => {
  const isReceived = perspective === "received";
  const userPrefix = isReceived ? "sender" : "receiver";
  const status = mapOnlineStatus(isReceived ? "online" : "offline");
  return {
    id: item.request_id,
    userId: item[`${userPrefix}_id`],
    name: item[`${userPrefix}_username`],
    photo: item[`${userPrefix}_photo`] || defaultAvatar,
    status,
    statusLabel: buildStatusLabel(status),
    signature: `${item[`${userPrefix}_username`]} 当前积分 ${item[`${userPrefix}_rating`]}`,
    message: item.message || "来一局吗？",
    rating: item[`${userPrefix}_rating`] || 0,
    region: "暂未公开",
    timeLabel: safeDateLabel(item.created_at, "刚刚"),
    tags: [requestStatusLabelMap[item.status] || item.status],
    requestStatus: item.status,
  };
};

/**
 * 处理 mapDiscoverItem 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of mapDiscoverItem, including state updates, interaction orchestration, and error branches.
 */
const mapDiscoverItem = (item) => {
  const relationMap = {
    none: "none",
    friend: "friend",
    sent_pending: "pending",
    received_pending: "incoming",
    self: "self",
  };
  return {
    id: item.user_id,
    name: item.username,
    photo: item.photo || defaultAvatar,
    signature: `当前积分 ${item.rating}，状态 ${buildStatusLabel(mapOnlineStatus(item.online_status))}`,
    rating: item.rating,
    region: "暂未公开",
    tags: [buildStatusLabel(mapOnlineStatus(item.online_status))],
    relation: relationMap[item.relation_status] || "none",
    requestId: item.request_id || null,
  };
};

/**
 * 处理 mapChatConversation 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of mapChatConversation, including state updates, interaction orchestration, and error branches.
 */
const mapChatConversation = (item) => {
  const status = mapOnlineStatus(item.online_status);
  return {
    friendId: item.friend_id,
    name: item.friend_username,
    photo: item.friend_photo || defaultAvatar,
    rating: item.friend_rating || 0,
    status,
    statusLabel: buildStatusLabel(status),
    lastMessage: item.last_message || "",
    lastMessageAt: safeDateLabel(item.last_message_at, "暂无消息"),
    lastMessageSenderId: item.last_message_sender_id ?? null,
    unreadCount: Number(item.unread_count) || 0,
  };
};

const mapChatMessage = (item) => ({
  id: item.message_id ?? item.id,
  senderId: item.sender_id ?? item.senderId,
  senderName: item.sender_username ?? item.senderName ?? "未知玩家",
  senderPhoto: item.sender_photo ?? item.senderPhoto ?? defaultAvatar,
  receiverId: item.receiver_id ?? item.receiverId,
  receiverName: item.receiver_username ?? item.receiverName ?? "未知玩家",
  receiverPhoto: item.receiver_photo ?? item.receiverPhoto ?? defaultAvatar,
  content: item.content || "",
  createdAt: safeDateLabel(item.created_at ?? item.createdAt, "刚刚"),
  isRead: Boolean(item.is_read ?? item.isRead),
});

const createDefaultStats = () => ({
  friends_count: 0,
  online_count: 0,
  pending_received_count: 0,
  pending_sent_count: 0,
  today_new_count: 0,
});

/**
 * 处理 ensureSuccess 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of ensureSuccess, including state updates, interaction orchestration, and error branches.
 */
const ensureSuccess = (resp, fallbackMessage) => {
  if (!resp || resp.error_message !== "success") {
    throw new Error(resp?.error_message || fallbackMessage);
  }
  return resp;
};

export const useFriendStore = defineStore("friend", {
  state: () => ({
    initialized: false,
    loading: false,
    discoverLoading: false,
    activeSegment: "all",
    activeTab: "detail",
    sidebarKeyword: "",
    discoverKeyword: "",
    discoverDisplayLimit: DISCOVER_DISPLAY_LIMIT,
    discoverTotalCount: 0,
    selectedFriendId: null,
    stats: createDefaultStats(),
    feedback: {
      visible: false,
      type: "info",
      title: "",
      message: "",
    },
    inviteDialogVisible: false,
    inviteDialogFriendId: null,
    inviteDialogBotId: "-1",
    inviteDialogLoading: false,
    inviteDialogSubmitting: false,
    inviteBots: [],
    chatDrawerVisible: false,
    chatConversationLoading: false,
    chatHistoryLoading: false,
    chatSending: false,
    chatDraft: "",
    chatActiveFriendId: null,
    chatMessages: [],
    chatMessagesCount: 0,
    chatConversations: [],
    pendingRemoveFriendId: null,
    friends: [],
    incomingRequests: [],
    outgoingRequests: [],
    discoverUsers: [],
  }),
  getters: {
    /**
     * 处理 summaryCards 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of summaryCards, including state updates, interaction orchestration, and error branches.
     */
    summaryCards(state) {
      return [
        {
          label: "好友总数",
          value: state.stats.friends_count,
          footnote: "当前好友",
        },
        {
          label: "在线好友",
          value: state.stats.online_count,
          footnote: "可立即开打",
        },
        {
          label: "待处理申请",
          value: state.stats.pending_received_count,
          footnote: "等你处理",
        },
        {
          label: "今日新增",
          value: state.stats.today_new_count,
          footnote: "今天新加",
        },
      ];
    },
    /**
     * 处理 segmentCounts 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of segmentCounts, including state updates, interaction orchestration, and error branches.
     */
    segmentCounts(state) {
      return {
        all: state.friends.length,
        online: state.friends.filter((friend) => friend.status !== "offline").length,
        recent: state.friends.filter((friend) => friend.recentlyPlayed).length,
        favorite: state.friends.filter((friend) => friend.favorite).length,
      };
    },
    /**
     * 处理 filteredFriends 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of filteredFriends, including state updates, interaction orchestration, and error branches.
     */
    filteredFriends(state) {
      const keyword = normalizeKeyword(state.sidebarKeyword);
      let friends = [...state.friends];

      if (state.activeSegment === "online") {
        friends = friends.filter((friend) => friend.status !== "offline");
      }
      if (state.activeSegment === "recent") {
        friends = friends.filter((friend) => friend.recentlyPlayed);
      }
      if (state.activeSegment === "favorite") {
        friends = friends.filter((friend) => friend.favorite);
      }
      if (keyword) {
        friends = friends.filter((friend) =>
          matchesKeyword(keyword, [friend.name, friend.signature, friend.note, friend.lastSeen])
        );
      }

      return friends.sort((left, right) => {
        if (left.favorite !== right.favorite) return Number(right.favorite) - Number(left.favorite);
        if (left.status !== right.status) return statusOrder[left.status] - statusOrder[right.status];
        return right.rating - left.rating;
      });
    },
    /**
     * 处理 selectedFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of selectedFriend, including state updates, interaction orchestration, and error branches.
     */
    selectedFriend(state) {
      return state.friends.find((friend) => friend.id === state.selectedFriendId) || state.friends[0] || null;
    },
    /**
     * 处理 pendingRemoveFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of pendingRemoveFriend, including state updates, interaction orchestration, and error branches.
     */
    pendingRemoveFriend(state) {
      return state.friends.find((friend) => friend.id === state.pendingRemoveFriendId) || null;
    },
    /**
     * 处理 inviteDialogFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of inviteDialogFriend, including state updates, interaction orchestration, and error branches.
     */
    inviteDialogFriend(state) {
      return state.friends.find((friend) => friend.id === state.inviteDialogFriendId) || null;
    },
    /**
     * 处理 filteredDiscoverUsers 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of filteredDiscoverUsers, including state updates, interaction orchestration, and error branches.
     */
    filteredDiscoverUsers(state) {
      const keyword = normalizeKeyword(state.discoverKeyword);
      return state.discoverUsers
        .filter((user) => user.relation !== "self")
        .filter((user) => matchesKeyword(keyword, [user.name, user.signature, user.region, ...(user.tags || [])]))
        .sort((left, right) => {
          if (left.relation !== right.relation) return relationOrder[left.relation] - relationOrder[right.relation];
          return right.rating - left.rating;
        })
        .slice(0, state.discoverDisplayLimit);
    },
    /**
     * 处理 activeChatFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of activeChatFriend, including state updates, interaction orchestration, and error branches.
     */
    activeChatFriend(state) {
      const targetId = state.chatActiveFriendId || state.selectedFriendId;
      return state.friends.find((friend) => friend.id === targetId) || null;
    },
    /**
     * 处理 activeChatConversation 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of activeChatConversation, including state updates, interaction orchestration, and error branches.
     */
    activeChatConversation(state) {
      const targetId = state.chatActiveFriendId || state.selectedFriendId;
      return state.chatConversations.find((conversation) => conversation.friendId === targetId) || null;
    },
  },
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
     * 处理 ensureInitialized 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of ensureInitialized with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param force 输入参数；Input parameter.
     */
    async ensureInitialized(force = false) {
      if (this.initialized && !force) return;
      this.loading = true;
      try {
        await Promise.all([
          this.fetchStats(),
          this.fetchFriends(),
          this.fetchReceivedRequests(),
          this.fetchSentRequests(),
          this.fetchChatConversations(),
        ]);
        await this.searchUsers(this.discoverKeyword);
        this.initialized = true;
      } catch (error) {
        this.showFeedback({
          title: "好友系统加载失败",
          message: error.message || "加载失败，请稍后重试。",
          type: "warn",
        });
        throw error;
      } finally {
        this.loading = false;
      }
    },
    /**
     * 处理 fetchStats 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchStats with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchStats() {
      const resp = ensureSuccess(
        await apiRequest(API_PATHS.friendStats, { token: this.getToken() }),
        "Load friend stats failed"
      );
      this.stats = {
        ...createDefaultStats(),
        ...(resp.stats || {}),
      };
    },
    /**
     * 处理 fetchFriends 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchFriends with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchFriends() {
      const resp = ensureSuccess(
        await apiRequest(API_PATHS.friendList, {
          token: this.getToken(),
          data: {
            page: 1,
            page_size: 50,
            sort_by: "created_at_desc",
          },
        }),
        "Load friend list failed"
      );
      this.friends = (resp.friends || []).map(mapFriendItem);
      if (!this.friends.some((friend) => friend.id === this.selectedFriendId)) {
        this.selectedFriendId = this.friends[0]?.id || null;
      }
    },
    /**
     * 处理 fetchReceivedRequests 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchReceivedRequests with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchReceivedRequests() {
      const resp = ensureSuccess(
        await apiRequest(API_PATHS.friendRequestReceived, {
          token: this.getToken(),
          data: { status: "pending", page: 1, page_size: 20 },
        }),
        "Load received requests failed"
      );
      this.incomingRequests = (resp.requests || []).map((item) => mapRequestItem(item, "received"));
    },
    /**
     * 处理 fetchSentRequests 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchSentRequests with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchSentRequests() {
      const resp = ensureSuccess(
        await apiRequest(API_PATHS.friendRequestSent, {
          token: this.getToken(),
          data: { status: "pending", page: 1, page_size: 20 },
        }),
        "Load sent requests failed"
      );
      this.outgoingRequests = (resp.requests || []).map((item) => mapRequestItem(item, "sent"));
    },
    /**
     * 处理 searchUsers 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of searchUsers with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param keyword 输入参数；Input parameter.
     */
    async searchUsers(keyword = this.discoverKeyword) {
      this.discoverLoading = true;
      try {
        const resp = ensureSuccess(
          await apiRequest(API_PATHS.friendSearch, {
            token: this.getToken(),
            data: {
              keyword: keyword || "",
              page: 1,
              page_size: this.discoverDisplayLimit,
            },
          }),
          "Search users failed"
        );
        this.discoverUsers = (resp.users || []).map(mapDiscoverItem);
        this.discoverTotalCount = Number(resp.users_count) || this.discoverUsers.length;
      } finally {
        this.discoverLoading = false;
      }
    },
    /**
     * 处理 refreshRelationshipData 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of refreshRelationshipData with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async refreshRelationshipData() {
      await Promise.all([
        this.fetchStats(),
        this.fetchFriends(),
        this.fetchReceivedRequests(),
        this.fetchSentRequests(),
      ]);
      await this.searchUsers(this.discoverKeyword);
    },
    /**
     * 处理 fetchInviteBots 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchInviteBots with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param force 输入参数；Input parameter.
     */
    async fetchInviteBots(force = false) {
      if (!force && this.inviteBots.length) return;
      this.inviteDialogLoading = true;
      try {
        const resp = await apiRequest(API_PATHS.botList, {
          token: this.getToken(),
        });
        this.inviteBots = (Array.isArray(resp) ? resp : []).map(normalizeBotOption);
      } finally {
        this.inviteDialogLoading = false;
      }
    },
    /**
     * 处理 fetchChatConversations 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchChatConversations with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async fetchChatConversations() {
      this.chatConversationLoading = true;
      try {
        const resp = ensureSuccess(
          await apiRequest(API_PATHS.friendChatConversations, {
            token: this.getToken(),
          }),
          "Load chat conversations failed"
        );
        this.chatConversations = (resp.conversations || []).map(mapChatConversation);
      } finally {
        this.chatConversationLoading = false;
      }
    },
    /**
     * 处理 fetchChatHistory 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of fetchChatHistory with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     * @param page 输入参数；Input parameter.
     */
    async fetchChatHistory(friendId, page = 1) {
      if (!friendId) return;
      this.chatHistoryLoading = true;
      try {
        const resp = ensureSuccess(
          await apiRequest(API_PATHS.friendChatHistory, {
            token: this.getToken(),
            data: {
              friend_id: friendId,
              page,
              page_size: CHAT_HISTORY_PAGE_SIZE,
            },
          }),
          "Load chat history failed"
        );
        this.chatMessages = (resp.messages || []).map(mapChatMessage);
        this.chatMessagesCount = Number(resp.messages_count) || this.chatMessages.length;
        this.markLocalConversationRead(friendId);
      } finally {
        this.chatHistoryLoading = false;
      }
    },
    /**
     * 处理 setActiveSegment 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setActiveSegment, including state updates, interaction orchestration, and error branches.
     */
    setActiveSegment(segment) {
      this.activeSegment = segment;
    },
    /**
     * 处理 setActiveTab 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setActiveTab, including state updates, interaction orchestration, and error branches.
     */
    setActiveTab(tab) {
      this.activeTab = tab;
    },
    /**
     * 处理 setSidebarKeyword 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setSidebarKeyword, including state updates, interaction orchestration, and error branches.
     */
    setSidebarKeyword(keyword) {
      this.sidebarKeyword = keyword;
    },
    /**
     * 处理 setDiscoverKeyword 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setDiscoverKeyword, including state updates, interaction orchestration, and error branches.
     */
    setDiscoverKeyword(keyword) {
      this.discoverKeyword = keyword;
      if (discoverSearchTimer) clearTimeout(discoverSearchTimer);
      this.discoverLoading = true;
      discoverSearchTimer = setTimeout(() => {
        this.searchUsers(this.discoverKeyword).catch((error) => {
          this.showFeedback({
            title: "搜索失败",
            message: error.message || "搜索失败，请稍后重试。",
            type: "warn",
          });
        });
      }, 280);
    },
    /**
     * 处理 selectFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of selectFriend, including state updates, interaction orchestration, and error branches.
     */
    selectFriend(friendId) {
      const target = this.friends.find((friend) => friend.id === friendId);
      if (!target) return;
      this.selectedFriendId = friendId;
      this.activeTab = "detail";
    },
    /**
     * 处理 showFeedback 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of showFeedback, including state updates, interaction orchestration, and error branches.
     */
    showFeedback({ title, message, type = "info" }) {
      this.feedback = { visible: true, title, message, type };
      if (feedbackTimer) clearTimeout(feedbackTimer);
      feedbackTimer = setTimeout(() => {
        this.clearFeedback();
      }, 2600);
    },
    /**
     * 处理 clearFeedback 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of clearFeedback, including state updates, interaction orchestration, and error branches.
     */
    clearFeedback() {
      if (feedbackTimer) {
        clearTimeout(feedbackTimer);
        feedbackTimer = null;
      }
      this.feedback.visible = false;
    },
    /**
     * 处理 getOutgoingInviteForFriend 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of getOutgoingInviteForFriend, including state updates, interaction orchestration, and error branches.
     */
    getOutgoingInviteForFriend(friendId) {
      return useRealtimeStore().getPendingInviteWithFriend(friendId);
    },
    /**
     * 处理 inviteFriend 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of inviteFriend with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async inviteFriend(friendId) {
      this.inviteDialogFriendId = friendId;
      this.inviteDialogVisible = true;
      this.inviteDialogBotId = usePkStore().selectedBotId || "-1";
      try {
        await Promise.all([
          this.fetchInviteBots(),
          useRealtimeStore().fetchPendingInvites(),
        ]);
      } catch (error) {
        this.showFeedback({
          title: "邀战信息加载失败",
          message: error.message || "暂时无法加载出战配置。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 closeInviteDialog 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of closeInviteDialog, including state updates, interaction orchestration, and error branches.
     */
    closeInviteDialog() {
      this.inviteDialogVisible = false;
      this.inviteDialogFriendId = null;
      this.inviteDialogSubmitting = false;
    },
    /**
     * 处理 setInviteDialogBot 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setInviteDialogBot, including state updates, interaction orchestration, and error branches.
     */
    setInviteDialogBot(botId) {
      this.inviteDialogBotId = String(botId ?? "-1");
    },
    /**
     * 处理 submitInviteDialog 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of submitInviteDialog with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async submitInviteDialog() {
      const friend = this.inviteDialogFriend;
      if (!friend || this.inviteDialogSubmitting) return;
      this.inviteDialogSubmitting = true;
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendInviteSend, {
            method: "POST",
            token: this.getToken(),
            data: {
              receiver_id: friend.id,
              sender_bot_id: Number.parseInt(this.inviteDialogBotId, 10),
              game_mode: "pk",
            },
          }),
          "Send invite failed"
        );
        await useRealtimeStore().fetchPendingInvites();
        this.closeInviteDialog();
        this.showFeedback({
          title: "邀战已发出",
          message: `已向 ${friend.name} 发出好友对战邀请。`,
          type: "success",
        });
      } catch (error) {
        this.showFeedback({
          title: "邀战发送失败",
          message: error.message || "发送失败，请稍后再试。",
          type: "warn",
        });
      } finally {
        this.inviteDialogSubmitting = false;
      }
    },
    /**
     * 处理 cancelInviteForFriend 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of cancelInviteForFriend with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async cancelInviteForFriend(friendId = this.inviteDialogFriendId) {
      const friend = this.friends.find((item) => item.id === friendId);
      const pendingInvite = this.getOutgoingInviteForFriend(friendId);
      if (!friend || !pendingInvite) return;
      try {
        await useRealtimeStore().cancelOutgoingInvite(pendingInvite.id);
        this.closeInviteDialog();
      } catch (error) {
        this.showFeedback({
          title: "撤回失败",
          message: error.message || "邀战撤回失败。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 sendQuickMessage 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of sendQuickMessage with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async sendQuickMessage(friendId) {
      await this.openChatDrawer(friendId);
    },
    /**
     * 处理 openChatDrawer 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of openChatDrawer with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async openChatDrawer(friendId = this.selectedFriendId) {
      this.chatDrawerVisible = true;
      await this.fetchChatConversations();
      const nextFriendId = friendId || this.chatConversations[0]?.friendId || this.friends[0]?.id || null;
      if (!nextFriendId) return;
      await this.setActiveChatFriend(nextFriendId);
    },
    /**
     * 处理 closeChatDrawer 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of closeChatDrawer, including state updates, interaction orchestration, and error branches.
     */
    closeChatDrawer() {
      this.chatDrawerVisible = false;
      this.chatDraft = "";
    },
    /**
     * 处理 setActiveChatFriend 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setActiveChatFriend with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async setActiveChatFriend(friendId) {
      if (!friendId) return;
      this.chatActiveFriendId = friendId;
      await this.fetchChatHistory(friendId);
      await this.markChatConversationRead(friendId);
    },
    /**
     * 处理 setChatDraft 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of setChatDraft, including state updates, interaction orchestration, and error branches.
     */
    setChatDraft(value) {
      this.chatDraft = value;
    },
    /**
     * 处理 markLocalConversationRead 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of markLocalConversationRead, including state updates, interaction orchestration, and error branches.
     */
    markLocalConversationRead(friendId) {
      const target = this.chatConversations.find((item) => item.friendId === friendId);
      if (target) {
        target.unreadCount = 0;
      }
    },
    /**
     * 处理 upsertConversationFromMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of upsertConversationFromMessage, including state updates, interaction orchestration, and error branches.
     */
    upsertConversationFromMessage(message) {
      const currentUserId = Number.parseInt(useUserStore().id, 10);
      const friendId = message.senderId === currentUserId ? message.receiverId : message.senderId;
      const friend =
        this.friends.find((item) => item.id === friendId)
        || {
          id: friendId,
          name: message.senderId === currentUserId ? message.receiverName : message.senderName,
          photo: message.senderId === currentUserId ? message.receiverPhoto : message.senderPhoto,
          rating: 0,
          status: "offline",
          statusLabel: "离线",
        };

      const existing = this.chatConversations.find((item) => item.friendId === friendId);
      const unreadIncrement =
        message.senderId !== currentUserId && this.chatActiveFriendId !== friendId ? 1 : 0;

      if (existing) {
        existing.lastMessage = message.content;
        existing.lastMessageAt = message.createdAt;
        existing.lastMessageSenderId = message.senderId;
        existing.unreadCount = this.chatActiveFriendId === friendId ? 0 : existing.unreadCount + unreadIncrement;
        this.chatConversations = [
          existing,
          ...this.chatConversations.filter((item) => item.friendId !== friendId),
        ];
        return;
      }

      this.chatConversations = [
        {
          friendId,
          name: friend.name,
          photo: friend.photo,
          rating: friend.rating || 0,
          status: friend.status || "offline",
          statusLabel: friend.statusLabel || "离线",
          lastMessage: message.content,
          lastMessageAt: message.createdAt,
          lastMessageSenderId: message.senderId,
          unreadCount: unreadIncrement,
        },
        ...this.chatConversations,
      ];
    },
    /**
     * 处理 upsertChatMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of upsertChatMessage, including state updates, interaction orchestration, and error branches.
     */
    upsertChatMessage(message) {
      if (this.chatMessages.some((item) => item.id === message.id)) return;
      if (!this.chatActiveFriendId) return;
      const currentUserId = Number.parseInt(useUserStore().id, 10);
      const targetFriendId = message.senderId === currentUserId ? message.receiverId : message.senderId;
      if (targetFriendId !== this.chatActiveFriendId) return;
      this.chatMessages = [...this.chatMessages, message];
      this.chatMessagesCount += 1;
    },
    /**
     * 处理 markChatConversationRead 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of markChatConversationRead with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async markChatConversationRead(friendId) {
      if (!friendId) return;
      try {
        await apiRequest(API_PATHS.friendChatRead, {
          method: "POST",
          token: this.getToken(),
          data: { friend_id: friendId },
        });
      } catch (error) {
        console.error("mark chat conversation read failed:", error);
      }
      this.markLocalConversationRead(friendId);
    },
    /**
     * 处理 sendActiveChatMessage 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of sendActiveChatMessage with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async sendActiveChatMessage() {
      const activeFriend = this.activeChatFriend;
      const content = this.chatDraft.trim();
      if (!activeFriend || !content || this.chatSending) return;

      this.chatSending = true;
      try {
        const resp = ensureSuccess(
          await apiRequest(API_PATHS.friendChatSend, {
            method: "POST",
            token: this.getToken(),
            data: {
              friend_id: activeFriend.id,
              content,
            },
          }),
          "Send chat message failed"
        );
        const message = mapChatMessage(resp.message || {});
        this.chatDraft = "";
        this.upsertChatMessage(message);
        this.upsertConversationFromMessage(message);
      } catch (error) {
        this.showFeedback({
          title: "发送失败",
          message: error.message || "消息发送失败，请重试。",
          type: "warn",
        });
      } finally {
        this.chatSending = false;
      }
    },
    /**
     * 处理 receivePrivateChatMessage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of receivePrivateChatMessage, including state updates, interaction orchestration, and error branches.
     */
    receivePrivateChatMessage(message) {
      const normalizedMessage = mapChatMessage(message);
      this.upsertConversationFromMessage(normalizedMessage);
      this.upsertChatMessage(normalizedMessage);
      if (this.chatDrawerVisible && this.chatActiveFriendId) {
        const currentUserId = Number.parseInt(useUserStore().id, 10);
        const friendId =
          normalizedMessage.senderId === currentUserId
            ? normalizedMessage.receiverId
            : normalizedMessage.senderId;
        if (friendId === this.chatActiveFriendId) {
          this.markChatConversationRead(friendId);
        }
      }
    },
    /**
     * 处理 toggleFavorite 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of toggleFavorite with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param friendId 输入参数；Input parameter.
     */
    async toggleFavorite(friendId) {
      const friend = this.friends.find((item) => item.id === friendId);
      if (!friend) return;
      try {
        const resp = ensureSuccess(
          await apiRequest(API_PATHS.friendFavoriteToggle, {
            method: "POST",
            token: this.getToken(),
            data: { friend_id: friendId },
          }),
          "Toggle favorite failed"
        );
        friend.favorite = Boolean(resp.is_favorite);
        await this.fetchStats();
        this.showFeedback({
          title: friend.favorite ? "已收藏好友" : "已取消收藏",
          message: friend.favorite ? `${friend.name} 已加入收藏。` : `${friend.name} 已取消收藏。`,
          type: friend.favorite ? "success" : "info",
        });
      } catch (error) {
        this.showFeedback({
          title: "更新失败",
          message: error.message || "操作失败，请稍后重试。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 openRemoveDialog 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of openRemoveDialog, including state updates, interaction orchestration, and error branches.
     */
    openRemoveDialog(friendId) {
      this.pendingRemoveFriendId = friendId;
    },
    /**
     * 处理 closeRemoveDialog 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of closeRemoveDialog, including state updates, interaction orchestration, and error branches.
     */
    closeRemoveDialog() {
      this.pendingRemoveFriendId = null;
    },
    /**
     * 处理 confirmRemoveFriend 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of confirmRemoveFriend with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
    async confirmRemoveFriend() {
      const friend = this.friends.find((item) => item.id === this.pendingRemoveFriendId);
      if (!friend) {
        this.pendingRemoveFriendId = null;
        return;
      }
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendRemove, {
            method: "POST",
            token: this.getToken(),
            data: { friend_id: friend.id },
          }),
          "Remove friend failed"
        );
        this.pendingRemoveFriendId = null;
        await this.refreshRelationshipData();
        this.showFeedback({
          title: "好友已移除",
          message: `${friend.name} 已从当前好友列表中移除。`,
          type: "warn",
        });
      } catch (error) {
        this.showFeedback({
          title: "删除失败",
          message: error.message || "好友删除失败。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 acceptIncomingRequest 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of acceptIncomingRequest with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param requestId 输入参数；Input parameter.
     */
    async acceptIncomingRequest(requestId) {
      const request = this.incomingRequests.find((item) => item.id === requestId);
      if (!request) return;
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendRequestAccept, {
            method: "POST",
            token: this.getToken(),
            data: { request_id: requestId },
          }),
          "Accept friend request failed"
        );
        await this.refreshRelationshipData();
        const nextFriend = this.friends.find((friend) => friend.id === request.userId);
        if (nextFriend) {
          this.selectedFriendId = nextFriend.id;
          this.activeTab = "detail";
        }
        this.showFeedback({
          title: "申请已通过",
          message: `${request.name} 已进入好友列表。`,
          type: "success",
        });
      } catch (error) {
        this.showFeedback({
          title: "处理失败",
          message: error.message || "好友申请处理失败。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 ignoreIncomingRequest 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of ignoreIncomingRequest with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param requestId 输入参数；Input parameter.
     */
    async ignoreIncomingRequest(requestId) {
      const request = this.incomingRequests.find((item) => item.id === requestId);
      if (!request) return;
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendRequestIgnore, {
            method: "POST",
            token: this.getToken(),
            data: { request_id: requestId },
          }),
          "Ignore friend request failed"
        );
        await this.refreshRelationshipData();
        this.showFeedback({
          title: "申请已忽略",
          message: `${request.name} 的申请已从待处理列表移除。`,
          type: "info",
        });
      } catch (error) {
        this.showFeedback({
          title: "处理失败",
          message: error.message || "忽略申请失败。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 sendFriendRequest 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of sendFriendRequest with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param userId 输入参数；Input parameter.
     */
    async sendFriendRequest(userId) {
      const user = this.discoverUsers.find((item) => item.id === userId);
      if (!user) return;
      if (user.relation === "incoming") {
        this.activeTab = "request";
        return;
      }
      if (user.relation === "pending" || user.relation === "friend" || user.relation === "self") {
        return;
      }
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendRequestSend, {
            method: "POST",
            token: this.getToken(),
            data: { receiver_id: userId, message: "来打一局？" },
          }),
          "Send friend request failed"
        );
        await this.refreshRelationshipData();
        this.showFeedback({
          title: "好友申请已发送",
          message: `已向 ${user.name} 发出好友申请。`,
          type: "success",
        });
      } catch (error) {
        this.showFeedback({
          title: "发送失败",
          message: error.message || "好友申请发送失败。",
          type: "warn",
        });
      }
    },
    /**
     * 处理 cancelOutgoingRequest 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of cancelOutgoingRequest with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param requestId 输入参数；Input parameter.
     */
    async cancelOutgoingRequest(requestId) {
      const request = this.outgoingRequests.find((item) => item.id === requestId);
      if (!request) return;
      try {
        ensureSuccess(
          await apiRequest(API_PATHS.friendRequestCancel, {
            method: "POST",
            token: this.getToken(),
            data: { request_id: requestId },
          }),
          "Cancel friend request failed"
        );
        await this.refreshRelationshipData();
        this.showFeedback({
          title: "申请已撤回",
          message: `${request.name} 的好友申请已撤回。`,
          type: "warn",
        });
      } catch (error) {
        this.showFeedback({
          title: "撤回失败",
          message: error.message || "好友申请撤回失败。",
          type: "warn",
        });
      }
    },
  },
});
