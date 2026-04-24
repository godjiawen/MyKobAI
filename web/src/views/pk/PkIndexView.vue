<template>
  <div class="pk-page">
    <transition name="invite-slide">
      <section v-if="incomingInvite" class="invite-banner invite-banner--incoming">
        <div class="invite-banner__media">
          <img :src="incomingInvite.senderPhoto" :alt="incomingInvite.senderUsername" class="invite-banner__avatar" />
          <div>
            <p class="invite-banner__eyebrow">好友邀战</p>
            <h3>{{ incomingInvite.senderUsername }} 邀请你开始一场 PK 对战</h3>
            <p class="invite-banner__meta">
              对方出战设置：{{ incomingInvite.senderBotTitle }}
              <span>你将使用当前设置应战：{{ selectedBotLabel }}</span>
            </p>
            <p class="invite-banner__meta">
              {{ incomingInvite.mapName }} · {{ incomingInvite.roundSeconds }} 秒
              <span>{{ incomingInvite.allowSpectator ? "允许观战" : "关闭观战" }}</span>
            </p>
            <p class="invite-banner__meta">邀请有效期至 {{ incomingInvite.expiredAt || "稍后失效" }}</p>
          </div>
        </div>
        <div class="invite-banner__actions">
          <button
            type="button"
            class="btn btn-primary invite-action"
            :disabled="inviteActionLoading"
            @click="respondInvite('accept')"
          >
            {{ inviteActionLoading ? "处理中..." : "接受邀战" }}
          </button>
          <button
            type="button"
            class="btn btn-outline-secondary invite-action"
            :disabled="inviteActionLoading"
            @click="respondInvite('reject')"
          >
            拒绝
          </button>
        </div>
      </section>
    </transition>

    <transition name="invite-slide">
      <section v-if="!incomingInvite && outgoingInvite" class="invite-banner invite-banner--outgoing">
        <div class="invite-banner__media">
          <img :src="outgoingInvite.receiverPhoto" :alt="outgoingInvite.receiverUsername" class="invite-banner__avatar" />
          <div>
            <p class="invite-banner__eyebrow">等待回应</p>
            <h3>已向 {{ outgoingInvite.receiverUsername }} 发出邀战</h3>
            <p class="invite-banner__meta">你的出战设置：{{ outgoingInvite.senderBotTitle }}</p>
            <p class="invite-banner__meta">
              {{ outgoingInvite.mapName }} · {{ outgoingInvite.roundSeconds }} 秒
              <span>{{ outgoingInvite.allowSpectator ? "允许观战" : "关闭观战" }}</span>
            </p>
            <p class="invite-banner__meta">邀请有效期至 {{ outgoingInvite.expiredAt || "稍后失效" }}</p>
          </div>
        </div>
        <div class="invite-banner__actions">
          <button
            type="button"
            class="btn btn-outline-secondary invite-action"
            :disabled="inviteActionLoading"
            @click="cancelOutgoingInvite"
          >
            {{ inviteActionLoading ? "处理中..." : "撤回邀战" }}
          </button>
        </div>
      </section>
    </transition>

    <transition name="invite-slide">
      <section
        v-if="!incomingInvite && !outgoingInvite && inviteNotice.visible"
        class="invite-banner invite-banner--notice"
        :class="`invite-banner--${inviteNotice.type}`"
      >
        <div>
          <p class="invite-banner__eyebrow">邀战状态</p>
          <h3>{{ inviteNotice.title }}</h3>
          <p class="invite-banner__meta">{{ inviteNotice.message }}</p>
        </div>
        <button type="button" class="invite-dismiss" @click="clearInviteNotice">关闭</button>
      </section>
    </transition>

    <PlayGround v-if="pkStore.status === 'playing'" />
    <MatchGround v-if="pkStore.status === 'matching'" />
    <ResultBoard v-if="pkStore.resultVisible" />
    <div class="match-meta" v-if="pkStore.status === 'playing'" @mousedown.stop>
      <span>{{ matchTypeLabel }}</span>
      <span>{{ pkStore.mapName }}</span>
      <span>{{ pkStore.roundSeconds }} 秒/回合</span>
      <span>{{ pkStore.allowSpectator ? "可观战" : "不可观战" }}</span>
    </div>
    <div class="user-color" v-if="isAPlayer" @mousedown.stop>你在左下角</div>
    <div class="user-color" v-if="isBPlayer" @mousedown.stop>你在右上角</div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount } from "vue";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import { useRealtimeStore } from "@/store/realtime";
import MatchGround from "@/components/MatchGround.vue";
import PlayGround from "@/components/PlayGround.vue";
import ResultBoard from "@/components/ResultBoard.vue";
import defaultAvatar from "@/assets/images/default-avatar.svg";

const userStore = useUserStore();
const pkStore = usePkStore();
const recordStore = useRecordStore();
const realtimeStore = useRealtimeStore();
const { incomingInvite, outgoingInvite, inviteActionLoading, inviteNotice } = storeToRefs(realtimeStore);

const currentUserId = computed(() => Number.parseInt(userStore.id, 10));
const isAPlayer = computed(
  () => pkStore.status === "playing" && currentUserId.value === Number.parseInt(pkStore.a_id, 10)
);
const isBPlayer = computed(
  () => pkStore.status === "playing" && currentUserId.value === Number.parseInt(pkStore.b_id, 10)
);
const selectedBotLabel = computed(() => (pkStore.selectedBotId === "-1" ? "手动操作" : `Bot #${pkStore.selectedBotId}`));
const matchTypeLabel = computed(() => {
  if (pkStore.matchType === "friend_private") return "好友私人局";
  if (pkStore.matchType === "tournament") return "锦标赛";
  return "匹配对战";
});

const clearInviteNotice = () => realtimeStore.clearInviteNotice();
const respondInvite = async (action) => realtimeStore.respondInvite(action);
const cancelOutgoingInvite = async () => realtimeStore.cancelOutgoingInvite();

onMounted(() => {
  recordStore.updateIsRecord(false);
  if (pkStore.status !== "playing") {
    pkStore.updateLoser("none");
    pkStore.updateResultVisible(false);
    pkStore.updatePaused({ isPaused: false, pausedByUserId: null });
    // 非对局中时清除暂离状态，防止历史状态渗入新局
    pkStore.updateAwaySuspended(false, null, "");
    if (!pkStore.opponent_username) {
      pkStore.updateOpponent({
        username: "匹配对手",
        photo: defaultAvatar,
      });
    }
  }

  if (userStore.is_login) {
    realtimeStore.initialize();
    realtimeStore.fetchPendingInvites().catch((error) => {
      console.error("load pending invites failed:", error);
    });
  }

  // 上报进入 PK 页面（WebSocket 可能尚未就绪，稍等后发送）
  const sendEnter = () => {
    const socket = pkStore.socket;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ event: "game-enter" }));
      socket.send(JSON.stringify({ event: "sync-game" }));
    } else {
      setTimeout(sendEnter, 300);
    }
  };
  setTimeout(sendEnter, 400);
});

onBeforeUnmount(() => {
  // 离开 PK 页面时通知后端暂离
  if (pkStore.status === "playing" && pkStore.loser === "none") {
    const socket = pkStore.socket;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ event: "game-leave", reason: "route-leave" }));
    }
  }
});
</script>

<style scoped>
.pk-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.invite-banner {
  width: min(1080px, 92vw);
  margin: 12px auto 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.26);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 18px 38px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(10px);
}

.invite-banner--incoming {
  border-color: rgba(61, 174, 255, 0.34);
  background: linear-gradient(135deg, rgba(232, 245, 255, 0.95), rgba(255, 255, 255, 0.88));
}

.invite-banner--outgoing {
  border-color: rgba(255, 183, 50, 0.32);
  background: linear-gradient(135deg, rgba(255, 250, 235, 0.95), rgba(255, 255, 255, 0.88));
}

.invite-banner--success {
  border-color: rgba(34, 197, 94, 0.3);
}

.invite-banner--warn {
  border-color: rgba(245, 158, 11, 0.3);
}

.invite-banner__media {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.invite-banner__avatar {
  width: 68px;
  height: 68px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.22);
}

.invite-banner__eyebrow {
  margin: 0 0 6px;
  color: var(--kob-accent-strong);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.invite-banner h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.16rem;
  color: var(--kob-text);
}

.invite-banner__meta {
  margin: 8px 0 0;
  color: var(--kob-muted);
  line-height: 1.6;
}

.invite-banner__meta span {
  display: inline-block;
  margin-left: 12px;
}

.invite-banner__actions {
  display: flex;
  gap: 10px;
  flex: none;
}

.invite-action,
.invite-dismiss {
  border-radius: 999px;
  min-width: 116px;
}

.invite-dismiss {
  border: 1px solid rgba(90, 180, 255, 0.24);
  padding: 9px 14px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--kob-accent-strong);
  font-weight: 700;
}

div.user-color {
  text-align: center;
  color: var(--kob-text);
  font-size: 24px;
  font-weight: 600;
  width: fit-content;
  margin: 12px auto 0;
  border: 1px solid rgba(90, 180, 255, 0.4);
  border-radius: 999px;
  padding: 6px 16px;
  background: var(--kob-panel);
  box-shadow: 0 10px 20px rgba(0, 50, 100, 0.1);
  backdrop-filter: blur(8px);
  animation: tag-in 240ms ease;
}

.match-meta {
  width: min(1080px, 92vw);
  margin: 10px auto 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.match-meta span {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid rgba(90, 180, 255, 0.24);
  background: rgba(255, 255, 255, 0.78);
  color: var(--kob-text);
  font-size: 0.78rem;
  font-weight: 700;
}

.invite-slide-enter-active,
.invite-slide-leave-active {
  transition:
    opacity var(--motion-medium) var(--ease-out),
    transform var(--motion-medium) var(--ease-out);
}

.invite-slide-enter-from,
.invite-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@keyframes tag-in {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 991px) {
  .invite-banner {
    width: min(94vw, 680px);
    flex-direction: column;
    align-items: stretch;
  }

  .invite-banner__actions {
    width: 100%;
  }

  .invite-action,
  .invite-dismiss {
    width: 100%;
  }
}

@media (max-width: 767px) {
  .invite-banner__media {
    grid-template-columns: 1fr;
  }

  .invite-banner__meta span {
    display: block;
    margin-left: 0;
    margin-top: 4px;
  }

  .invite-banner__actions {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .invite-slide-enter-active,
  .invite-slide-leave-active,
  div.user-color {
    transition: none !important;
    animation: none !important;
  }
}
</style>
