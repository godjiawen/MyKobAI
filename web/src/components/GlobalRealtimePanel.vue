<template>
  <div v-if="!isPkRoute" class="realtime-panel">
    <transition name="realtime-pop">
      <section v-if="incomingInvite" class="realtime-card realtime-card--invite">
        <div class="realtime-head">
          <img :src="incomingInvite.senderPhoto" :alt="incomingInvite.senderUsername" class="realtime-avatar" />
          <div>
            <p class="realtime-kicker">好友邀战</p>
            <strong>{{ incomingInvite.senderUsername }} 正在等你应战</strong>
            <p>{{ incomingInvite.senderBotTitle }} 发起邀请，当前将用 {{ selectedBotLabel }} 应战。</p>
          </div>
        </div>
        <div class="realtime-actions">
          <button type="button" class="btn btn-primary realtime-btn" :disabled="inviteActionLoading" @click="realtimeStore.respondInvite('accept')">
            {{ inviteActionLoading ? '处理中...' : '接受' }}
          </button>
          <button type="button" class="btn btn-outline-secondary realtime-btn" :disabled="inviteActionLoading" @click="realtimeStore.respondInvite('reject')">
            拒绝
          </button>
          <button type="button" class="realtime-link" @click="goPk">去 PK 页</button>
        </div>
      </section>
    </transition>

    <transition name="realtime-pop">
      <section v-if="!incomingInvite && outgoingInvite" class="realtime-card realtime-card--pending">
        <div class="realtime-head">
          <img :src="outgoingInvite.receiverPhoto" :alt="outgoingInvite.receiverUsername" class="realtime-avatar" />
          <div>
            <p class="realtime-kicker">邀战进行中</p>
            <strong>已向 {{ outgoingInvite.receiverUsername }} 发出邀战</strong>
            <p>邀请仍在等待回应，可以前往 PK 页查看实时状态。</p>
          </div>
        </div>
        <div class="realtime-actions">
          <button type="button" class="btn btn-outline-secondary realtime-btn" :disabled="inviteActionLoading" @click="realtimeStore.cancelOutgoingInvite()">
            {{ inviteActionLoading ? '处理中...' : '撤回邀战' }}
          </button>
          <button type="button" class="realtime-link" @click="goPk">去 PK 页</button>
        </div>
      </section>
    </transition>

    <transition name="realtime-pop">
      <section v-if="!incomingInvite && !outgoingInvite && pkStore.status === 'playing'" class="realtime-card realtime-card--match">
        <div>
          <p class="realtime-kicker">对局已开始</p>
          <strong>你当前有一场进行中的 PK 对局</strong>
          <p>当前对手：{{ pkStore.opponent_username || '匹配对手' }}</p>
        </div>
        <div class="realtime-actions">
          <button type="button" class="btn btn-primary realtime-btn" @click="goPk">进入对局</button>
        </div>
      </section>
    </transition>

    <transition name="realtime-pop">
      <section v-if="friendRequestNotice.visible && friendRequestNotice.request" class="realtime-card realtime-card--request">
        <div class="realtime-head">
          <img :src="friendRequestNotice.request.senderPhoto" :alt="friendRequestNotice.request.senderUsername" class="realtime-avatar" />
          <div>
            <p class="realtime-kicker">好友申请</p>
            <strong>{{ friendRequestNotice.request.senderUsername }} 向你发送了好友申请</strong>
            <p>{{ friendRequestNotice.request.message }}</p>
          </div>
        </div>
        <div class="realtime-actions">
          <button type="button" class="btn btn-outline-primary realtime-btn" @click="goFriends">去处理</button>
          <button type="button" class="realtime-link" @click="realtimeStore.clearFriendRequestNotice()">关闭</button>
        </div>
      </section>
    </transition>

    <transition name="realtime-pop">
      <section v-if="friendMessageNotice.visible && friendMessageNotice.message" class="realtime-card realtime-card--message">
        <div class="realtime-head">
          <img :src="friendMessageNotice.message.senderPhoto" :alt="friendMessageNotice.message.senderUsername" class="realtime-avatar" />
          <div>
            <p class="realtime-kicker">好友私聊</p>
            <strong>{{ friendMessageNotice.message.senderUsername }} 发来新消息</strong>
            <p>{{ friendMessageNotice.message.content }}</p>
          </div>
        </div>
        <div class="realtime-actions">
          <button type="button" class="btn btn-outline-primary realtime-btn" @click="goFriends">去查看</button>
          <button type="button" class="realtime-link" @click="realtimeStore.clearFriendMessageNotice()">关闭</button>
        </div>
      </section>
    </transition>

    <transition name="realtime-pop">
      <section v-if="!incomingInvite && !outgoingInvite && !friendMessageNotice.visible && inviteNotice.visible" class="realtime-card realtime-card--notice" :class="`realtime-card--${inviteNotice.type}`">
        <div>
          <p class="realtime-kicker">实时状态</p>
          <strong>{{ inviteNotice.title }}</strong>
          <p>{{ inviteNotice.message }}</p>
        </div>
        <div class="realtime-actions">
          <button type="button" class="realtime-link" @click="realtimeStore.clearInviteNotice()">关闭</button>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { usePkStore } from "@/store/pk";
import { useRealtimeStore } from "@/store/realtime";

const route = useRoute();
const router = useRouter();
const pkStore = usePkStore();
const realtimeStore = useRealtimeStore();
const { incomingInvite, outgoingInvite, inviteActionLoading, inviteNotice, friendRequestNotice, friendMessageNotice } = storeToRefs(realtimeStore);

const isPkRoute = computed(() => route.name === "pk_index");
const selectedBotLabel = computed(() => (pkStore.selectedBotId === "-1" ? "手动操作" : `Bot #${pkStore.selectedBotId}`));

const goPk = async () => {
  await router.push({ name: "pk_index" });
};

const goFriends = async () => {
  realtimeStore.clearFriendRequestNotice();
  await router.push({ name: "friends_index" });
};
</script>

<style scoped>
.realtime-panel {
  position: fixed;
  top: 94px;
  right: 18px;
  z-index: 2500;
  width: min(380px, calc(100vw - 24px));
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.realtime-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 40px rgba(0, 50, 100, 0.12);
  backdrop-filter: blur(10px);
}

.realtime-card--invite {
  border-color: rgba(61, 174, 255, 0.34);
  background: linear-gradient(145deg, rgba(233, 246, 255, 0.96), rgba(255, 255, 255, 0.9));
}

.realtime-card--pending {
  border-color: rgba(255, 183, 50, 0.3);
  background: linear-gradient(145deg, rgba(255, 251, 235, 0.96), rgba(255, 255, 255, 0.9));
}

.realtime-card--success {
  border-color: rgba(34, 197, 94, 0.28);
}

.realtime-card--warn {
  border-color: rgba(245, 158, 11, 0.3);
}

.realtime-card--message {
  border-color: rgba(79, 70, 229, 0.24);
  background: linear-gradient(145deg, rgba(242, 244, 255, 0.96), rgba(255, 255, 255, 0.9));
}

.realtime-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.realtime-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
}

.realtime-kicker {
  margin: 0 0 6px;
  color: var(--kob-accent-strong);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.realtime-card strong {
  display: block;
  color: var(--kob-text);
}

.realtime-card p {
  margin: 6px 0 0;
  color: var(--kob-muted);
  line-height: 1.55;
}

.realtime-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.realtime-btn {
  border-radius: 999px;
  min-width: 96px;
}

.realtime-link {
  border: 0;
  padding: 8px 10px;
  background: transparent;
  color: var(--kob-accent-strong);
  font-weight: 700;
}

.realtime-pop-enter-active,
.realtime-pop-leave-active {
  transition:
    opacity var(--motion-medium) var(--ease-out),
    transform var(--motion-medium) var(--ease-out);
}

.realtime-pop-enter-from,
.realtime-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

@media (max-width: 767px) {
  .realtime-panel {
    top: 82px;
    right: 12px;
    left: 12px;
    width: auto;
  }

  .realtime-head {
    grid-template-columns: 1fr;
  }

  .realtime-actions {
    flex-direction: column;
  }

  .realtime-btn,
  .realtime-link {
    width: 100%;
  }
}
</style>
