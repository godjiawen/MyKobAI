<template>
  <div class="friends-page">
    <section class="container friends-dashboard">
      <FriendsFeedbackBar :feedback="feedback" @close="friendStore.clearFeedback()" />
      <FriendsSummaryBar :cards="summaryCards" :loading="loading" />

      <div class="friends-layout" :class="{ 'is-loading': loading }" :aria-busy="loading">
        <FriendsSidebar
          :segments="segments"
          :active-segment="activeSegment"
          :search-value="sidebarKeyword"
          :friends="filteredFriends"
          :selected-friend-id="selectedFriendId"
          @open-discover="friendStore.setActiveTab('discover')"
          @update:search="friendStore.setSidebarKeyword($event)"
          @select-segment="friendStore.setActiveSegment($event)"
          @select-friend="friendStore.selectFriend($event)"
          @toggle-favorite="friendStore.toggleFavorite($event)"
        />

        <section class="friends-main">
          <header class="main-head">
            <div>
              <p class="panel-kicker">Workspace</p>
              <h3>好友中心</h3>
              <p class="main-description">查看详情、处理申请、发现玩家。</p>
            </div>
            <div class="main-controls">
              <div class="main-tabs" role="tablist" aria-label="好友系统主面板切换">
                <button
                  v-for="tab in tabs"
                  :key="tab.key"
                  type="button"
                  class="main-tab"
                  :class="{ active: activeTab === tab.key }"
                  :aria-selected="activeTab === tab.key"
                  @click="friendStore.setActiveTab(tab.key)"
                >
                  {{ tab.label }}
                </button>
              </div>
              <label v-if="activeTab === 'discover'" class="discover-search-shell" for="discover-main-search">
                <span>搜索用户</span>
                <input
                  id="discover-main-search"
                  type="text"
                  class="form-control discover-search-input"
                  :value="discoverKeyword"
                  placeholder="输入昵称或积分"
                  @input="friendStore.setDiscoverKeyword($event.target.value)"
                />
              </label>
            </div>
          </header>

          <div v-if="loading" class="main-loading-shell" aria-hidden="true">
            <div class="loading-banner shimmer-block"></div>
            <div class="loading-metrics">
              <span v-for="index in 4" :key="`metric-${index}`" class="loading-card shimmer-block"></span>
            </div>
            <div class="loading-grid">
              <span class="loading-column shimmer-block"></span>
              <span class="loading-column shimmer-block"></span>
            </div>
          </div>

          <transition v-else name="panel-swap" mode="out-in">
            <FriendDetailPanel
              v-if="activeTab === 'detail'"
              :key="'detail'"
              :friend="selectedFriend"
              @invite="friendStore.inviteFriend($event)"
              @message="friendStore.sendQuickMessage($event)"
              @toggle-favorite="friendStore.toggleFavorite($event)"
              @remove="friendStore.openRemoveDialog($event)"
              @open-discover="friendStore.setActiveTab('discover')"
            />
            <FriendRequestPanel
              v-else-if="activeTab === 'request'"
              :key="'request'"
              :incoming-requests="incomingRequests"
              :outgoing-requests="outgoingRequests"
              @accept="friendStore.acceptIncomingRequest($event)"
              @ignore="friendStore.ignoreIncomingRequest($event)"
              @cancel="friendStore.cancelOutgoingRequest($event)"
            />
            <FriendDiscoverPanel
              v-else
              :key="'discover'"
              :loading="discoverLoading"
              :users="filteredDiscoverUsers"
              :display-limit="discoverDisplayLimit"
              :total-count="discoverTotalCount"
              @clear-search="friendStore.setDiscoverKeyword('')"
              @send-request="friendStore.sendFriendRequest($event)"
              @open-requests="friendStore.setActiveTab('request')"
              @open-friend="friendStore.selectFriend($event)"
            />
          </transition>
        </section>
      </div>
    </section>

    <AppDialog
      v-model="removeDialogVisible"
      title="删除好友"
      :message="removeDialogMessage"
      confirm-text="确认删除"
      cancel-text="取消"
      :show-cancel="true"
      @confirm="friendStore.confirmRemoveFriend()"
      @cancel="friendStore.closeRemoveDialog()"
    />

    <FriendInviteDialog
      v-model="inviteDialogVisible"
      :friend="inviteDialogFriend"
      :bots="inviteBots"
      :selected-bot-id="inviteDialogBotId"
      :loading="inviteDialogLoading"
      :submitting="inviteDialogSubmitting"
      :pending-invite="inviteDialogPendingInvite"
      @update:selectedBotId="friendStore.setInviteDialogBot($event)"
      @confirm="friendStore.submitInviteDialog()"
      @cancel-invite="friendStore.cancelInviteForFriend()"
    />

    <FriendChatDrawer
      v-model="chatDrawerVisible"
      :conversations="chatConversations"
      :active-friend="activeChatFriend"
      :active-conversation="activeChatConversation"
      :messages="chatMessages"
      :draft="chatDraft"
      :loading-conversations="chatConversationLoading"
      :loading-messages="chatHistoryLoading"
      :sending="chatSending"
      @select-friend="friendStore.setActiveChatFriend($event)"
      @update:draft="friendStore.setChatDraft($event)"
      @send="friendStore.sendActiveChatMessage()"
    />
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import AppDialog from "@/components/AppDialog.vue";
import { useFriendStore } from "@/store/friend";
import { useRealtimeStore } from "@/store/realtime";
import FriendsFeedbackBar from "./components/FriendsFeedbackBar.vue";
import FriendsSidebar from "./components/FriendsSidebar.vue";
import FriendsSummaryBar from "./components/FriendsSummaryBar.vue";
import FriendDetailPanel from "./components/FriendDetailPanel.vue";
import FriendRequestPanel from "./components/FriendRequestPanel.vue";
import FriendDiscoverPanel from "./components/FriendDiscoverPanel.vue";
import FriendInviteDialog from "./components/FriendInviteDialog.vue";
import FriendChatDrawer from "./components/FriendChatDrawer.vue";

const friendStore = useFriendStore();
const realtimeStore = useRealtimeStore();
const {
  activeSegment,
  activeTab,
  sidebarKeyword,
  discoverKeyword,
  loading,
  selectedFriendId,
  selectedFriend,
  discoverLoading,
  discoverDisplayLimit,
  discoverTotalCount,
  summaryCards,
  segmentCounts,
  filteredFriends,
  incomingRequests,
  outgoingRequests,
  filteredDiscoverUsers,
  feedback,
  pendingRemoveFriend,
  inviteDialogVisible,
  inviteDialogFriend,
  inviteDialogBotId,
  inviteDialogLoading,
  inviteDialogSubmitting,
  inviteBots,
  chatDrawerVisible,
  chatConversationLoading,
  chatHistoryLoading,
  chatSending,
  chatDraft,
  chatMessages,
  chatConversations,
  activeChatFriend,
  activeChatConversation,
} = storeToRefs(friendStore);

const segments = computed(() => [
  { key: "all", label: "全部好友", count: segmentCounts.value.all },
  { key: "online", label: "在线", count: segmentCounts.value.online },
  { key: "recent", label: "最近互动", count: segmentCounts.value.recent },
  { key: "favorite", label: "已收藏", count: segmentCounts.value.favorite },
]);

const tabs = computed(() => [
  { key: "detail", label: "详情" },
  { key: "request", label: `申请 (${incomingRequests.value.length})` },
  { key: "discover", label: "发现用户" },
]);

const removeDialogVisible = computed({
  get: () => Boolean(pendingRemoveFriend.value),
  set: (visible) => {
    if (!visible) friendStore.closeRemoveDialog();
  },
});

const removeDialogMessage = computed(() => {
  if (!pendingRemoveFriend.value) return "";
  return `确认删除好友 ${pendingRemoveFriend.value.name} 吗？`;
});

const inviteDialogPendingInvite = computed(() =>
  inviteDialogFriend.value ? realtimeStore.getPendingInviteWithFriend(inviteDialogFriend.value.id) : null
);

onMounted(() => {
  friendStore.ensureInitialized();
});
</script>

<style scoped>
.friends-page {
  position: relative;
  padding-top: clamp(20px, 2.8vw, 34px);
  padding-bottom: 22px;
  --friends-border: rgba(90, 180, 255, 0.22);
  --friends-shadow: 0 18px 42px rgba(0, 50, 100, 0.08);
}

.friends-dashboard {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.panel-kicker {
  margin: 0 0 8px;
  color: var(--kob-accent-strong);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.main-head h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  color: var(--kob-text);
}

.friends-layout {
  display: grid;
  grid-template-columns: minmax(320px, 360px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.friends-layout.is-loading {
  pointer-events: none;
}

.friends-main {
  min-height: 100%;
  padding: 26px;
  border-radius: 30px;
  border: 1px solid var(--friends-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(243, 249, 255, 0.82));
  box-shadow: var(--friends-shadow);
  backdrop-filter: blur(10px);
}

.main-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 22px;
}

.main-description {
  max-width: 46ch;
  margin: 10px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.main-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.main-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.discover-search-shell {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--kob-muted);
  font-size: 0.8rem;
  font-weight: 700;
}

.discover-search-input {
  min-width: 260px;
  min-height: 44px;
  border-radius: 999px;
}

.main-tab {
  border: 1px solid rgba(90, 180, 255, 0.2);
  border-radius: 999px;
  padding: 9px 14px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--kob-muted);
  font-size: 0.8rem;
  font-weight: 700;
  transition:
    background-color var(--motion-fast) var(--ease-out),
    color var(--motion-fast) var(--ease-out),
    border-color var(--motion-fast) var(--ease-out),
    transform var(--motion-fast) var(--ease-out);
}

.main-tab.active,
.main-tab:hover {
  transform: translateY(-1px);
  border-color: rgba(90, 180, 255, 0.3);
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
}

.main-loading-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-banner,
.loading-card,
.loading-column {
  display: block;
  border-radius: 22px;
}

.loading-banner {
  height: 124px;
}

.loading-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.loading-card {
  height: 102px;
}

.loading-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(0, 0.94fr);
  gap: 14px;
}

.loading-column {
  min-height: 260px;
}

.shimmer-block {
  position: relative;
  overflow: hidden;
  background: rgba(219, 231, 244, 0.82);
}

.shimmer-block::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.72), transparent);
  animation: friends-shimmer 1.4s ease-in-out infinite;
}

.panel-swap-enter-active,
.panel-swap-leave-active {
  transition:
    opacity var(--motion-medium) var(--ease-out),
    transform var(--motion-medium) var(--ease-out);
}

.panel-swap-enter-from,
.panel-swap-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 1199px) {
  .friends-layout {
    grid-template-columns: 1fr;
  }

  .loading-metrics,
  .loading-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 991px) {
  .friends-page {
    padding-top: 18px;
  }

  .main-head {
    flex-direction: column;
    align-items: stretch;
  }

  .friends-main {
    padding: 22px;
  }
}

@media (max-width: 767px) {
  .friends-page {
    padding-top: 14px;
  }

  .main-tabs {
    flex-direction: column;
  }

  .main-tab {
    width: 100%;
  }

  .main-controls {
    align-items: stretch;
  }

  .discover-search-shell {
    display: block;
  }

  .discover-search-shell span {
    display: block;
    margin-bottom: 6px;
  }

  .discover-search-input {
    width: 100%;
    min-width: 0;
  }

  .friends-main {
    padding: 18px;
    border-radius: 22px;
  }

  .loading-metrics,
  .loading-grid {
    grid-template-columns: 1fr;
  }
}

@keyframes friends-shimmer {
  to {
    transform: translateX(100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .main-tab,
  .panel-swap-enter-active,
  .panel-swap-leave-active,
  .shimmer-block::after {
    transition: none !important;
    animation: none !important;
  }
}
</style>
