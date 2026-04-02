<template>
  <!-- @mousedown.stop 阻止冒泡到 document，只有点到 battle-area 外才触发暂离 -->
  <div :class="['battle-area', { 'battle-area--record': store.state.record.is_record }]" @mousedown.stop>
    <div class="playground">
      <GameMap />
      <!-- 暂离遮罩 -->
      <div v-if="store.state.pk.isPaused" class="pause-overlay">
        <div class="pause-panel">
          <div class="pause-icon">⏸</div>
          <template v-if="isPausedByMe">
            <p class="pause-title">你暂离了对局</p>
            <p class="pause-sub">游戏已暂停，对手正在等你</p>
            <button class="btn-resume" @click="resumeGame">回到对局</button>
          </template>
          <template v-else>
            <p class="pause-title">对手暂时离开了</p>
            <p class="pause-sub">游戏已暂停，等待对手回来…</p>
          </template>
        </div>
      </div>
    </div>
    <ChatBox
      v-if="!store.state.record.is_record"
      :roomId="store.state.pk.roomId"
      @activity-change="handleChatActivityChange"
    />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useStore } from "vuex";
import GameMap from "@/components/GameMap.vue";
import ChatBox from "@/components/ChatBox.vue";

const store = useStore();

const isPausedByMe = computed(
  () =>
    store.state.pk.isPaused &&
    store.state.pk.pausedByUserId === Number.parseInt(store.state.user.id, 10)
);

const resumeGame = () => {
  const socket = store.state.pk.socket;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ event: "resume" }));
  }
};

const handleChatActivityChange = (active) => {
  const socket = store.state.pk.socket;
  if (!socket || socket.readyState !== WebSocket.OPEN) return;

  if (active) {
    if (!store.state.pk.isPaused && store.state.pk.loser === "none") {
      socket.send(JSON.stringify({ event: "pause" }));
    }
    return;
  }

  if (isPausedByMe.value && store.state.pk.loser === "none") {
    socket.send(JSON.stringify({ event: "resume" }));
  }
};
</script>

<style scoped>
.battle-area {
  display: flex;
  align-items: stretch;
  gap: 16px;
  width: min(1120px, 96vw);
  margin: 22px auto 40px;
}

.battle-area--record {
  width: min(820px, 96vw);
  justify-content: center;
}

div.playground {
  position: relative; /* 让遮罩绝对定位在此 div 内 */
  flex: 1 1 0;
  min-width: 0;
  height: clamp(400px, 68vh, 720px);
  border: 1px solid var(--kob-panel-border);
  border-radius: 22px;
  background: var(--kob-panel);
  box-shadow: 0 22px 48px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
  padding: 14px;
}

.battle-area--record div.playground {
  flex: 0 0 auto;
  width: min(96vw, calc(clamp(400px, 68vh, 720px) * 14 / 13));
  height: auto;
  aspect-ratio: 14 / 13;
}

/* 暂离遮罩 */
.pause-overlay {
  position: absolute;
  inset: 0;
  border-radius: 22px;
  background: rgba(220, 235, 255, 0.72);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  animation: fade-in 200ms ease;
}

@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.pause-panel {
  text-align: center;
  padding: 36px 44px;
  border-radius: 20px;
  border: 1px solid var(--kob-panel-border);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 20px 48px rgba(0, 50, 100, 0.14);
}

.pause-icon {
  font-size: 3rem;
  margin-bottom: 12px;
  line-height: 1;
}

.pause-title {
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--kob-text);
  margin: 0 0 6px;
}

.pause-sub {
  font-size: 0.9rem;
  color: var(--kob-muted);
  margin: 0 0 20px;
}

.btn-resume {
  border: none;
  border-radius: 999px;
  padding: 10px 30px;
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(61, 174, 255, 0.35);
  transition: transform 160ms ease;
}

.btn-resume:hover {
  transform: translateY(-2px);
}

/* 让 ChatBox 与 playground 等高 */
.battle-area :deep(.chat-box) {
  height: clamp(400px, 68vh, 720px);
}
</style>
