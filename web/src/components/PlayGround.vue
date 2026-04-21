<template>
  <div :class="['battle-area', 'kob-page-shell', { 'battle-area--record': recordStore.is_record }]" @mousedown.stop>
    <div class="playground">
      <div v-if="showPauseControl" class="play-controls">
        <button
          class="pause-toggle-btn"
          type="button"
          :disabled="isPauseButtonDisabled"
          @click="togglePause"
        >
          {{ pauseButtonText }}
        </button>
        <span class="pause-status">{{ pauseHintText }}</span>
      </div>

      <GameMap />

      <div v-if="pkStore.isPaused" class="pause-overlay">
        <div class="pause-panel">
          <div class="pause-icon">⏸</div>
          <template v-if="isPausedByMe">
            <p class="pause-title">你已暂停对局</p>
            <p class="pause-sub">游戏暂停中，对手正在等待你返回。</p>
            <button class="btn-resume" type="button" @click="resumeGame">继续对局</button>
          </template>
          <template v-else>
            <p class="pause-title">对手已暂停对局</p>
            <p class="pause-sub">游戏暂停中，请等待对手恢复。</p>
          </template>
        </div>
      </div>

      <!-- 暂离遮罩：主动暂停优先，暂离只在非暂停时显示 -->
      <div v-if="pkStore.isAwaySuspended && !pkStore.isPaused" class="pause-overlay away-overlay">
        <div class="pause-panel">
          <div class="pause-icon">🚶</div>
          <p class="pause-title">对手暂时离开对局</p>
          <p class="pause-sub">等待对方返回后继续……</p>
        </div>
      </div>
    </div>

    <ChatBox v-if="!recordStore.is_record" :roomId="pkStore.roomId" />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import GameMap from "@/components/GameMap.vue";
import ChatBox from "@/components/ChatBox.vue";

const userStore = useUserStore();
const pkStore = usePkStore();
const recordStore = useRecordStore();

const isPausedByMe = computed(
  () =>
    pkStore.isPaused &&
    pkStore.pausedByUserId === Number.parseInt(userStore.id, 10)
);

const isSocketReady = computed(
  () => pkStore.socket && pkStore.socket.readyState === WebSocket.OPEN
);

const showPauseControl = computed(
  () => !recordStore.is_record && pkStore.loser === "none"
);

const pauseButtonText = computed(() => {
  if (!pkStore.isPaused) return "暂停";
  if (isPausedByMe.value) return "继续";
  return "对手已暂停";
});

const pauseHintText = computed(() => {
  if (!isSocketReady.value) return "连接未就绪";
  if (!pkStore.isPaused) return "点击按钮可主动暂停";
  if (isPausedByMe.value) return "你可以手动继续对局";
  return "当前由对手暂停";
});

const isPauseButtonDisabled = computed(() => {
  if (!isSocketReady.value) return true;
  if (!pkStore.isPaused) return false;
  return !isPausedByMe.value;
});

/**
 * Handles resumeGame.
 * ??resumeGame?
 */
const resumeGame = () => {
  const socket = pkStore.socket;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ event: "resume" }));
  }
};

/**
 * Handles pauseGame.
 * ??pauseGame?
 */
const pauseGame = () => {
  const socket = pkStore.socket;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ event: "pause" }));
  }
};

/**
 * Handles togglePause.
 * ??togglePause?
 */
const togglePause = () => {
  if (pkStore.loser !== "none") return;

  if (!pkStore.isPaused) {
    pauseGame();
    return;
  }

  if (isPausedByMe.value) {
    resumeGame();
  }
};
</script>

<style scoped>
.battle-area {
  display: flex;
  align-items: stretch;
  gap: 16px;
  width: min(1120px, 96vw);
  margin: 12px auto 40px;
}

.battle-area--record {
  width: min(820px, 96vw);
  justify-content: center;
}

.playground {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  height: clamp(400px, 68vh, 720px);
  border: 1px solid rgba(90, 180, 255, 0.24);
  border-radius: var(--kob-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(244, 250, 255, 0.84)),
    radial-gradient(circle at top right, rgba(255, 208, 93, 0.1), transparent 36%);
  box-shadow: var(--kob-shadow-md);
  backdrop-filter: blur(12px);
  padding: 14px;
}

.play-controls {
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 55;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pause-toggle-btn {
  border: none;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  box-shadow: 0 6px 16px rgba(61, 174, 255, 0.35);
  transition:
    transform var(--motion-fast) var(--ease-out),
    opacity var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.pause-toggle-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.pause-toggle-btn:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.9),
    0 0 0 6px rgba(61, 174, 255, 0.28);
}

.pause-toggle-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pause-status {
  border-radius: 999px;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(90, 180, 255, 0.35);
  color: var(--kob-muted);
  font-size: 0.78rem;
  line-height: 1;
}

.battle-area--record .playground {
  flex: 0 0 auto;
  width: min(96vw, calc(clamp(400px, 68vh, 720px) * 14 / 13));
  height: auto;
  aspect-ratio: 14 / 13;
}

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
  animation: fade-in var(--motion-medium) var(--ease-out);
}

.away-overlay {
  background: rgba(255, 248, 220, 0.76);
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
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
  transition:
    transform var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.btn-resume:hover {
  transform: translateY(-2px);
}

.btn-resume:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.9),
    0 0 0 6px rgba(61, 174, 255, 0.28);
}

.battle-area :deep(.chat-box) {
  height: clamp(400px, 68vh, 720px);
}

@media (max-width: 1199px) {
  .battle-area {
    width: min(1000px, 96vw);
    gap: 12px;
  }

  .battle-area :deep(.chat-box) {
    height: clamp(340px, 58vh, 620px);
  }
}

@media (max-width: 991px) {
  .battle-area {
    flex-direction: column;
    gap: 12px;
    margin-top: 14px;
  }

  .playground {
    height: clamp(320px, 54vh, 520px);
    border-radius: 18px;
    padding: 10px;
  }

  .play-controls {
    top: 14px;
    right: 14px;
    gap: 8px;
  }

  .pause-status {
    display: none;
  }

  .battle-area--record .playground {
    width: min(96vw, 560px);
  }

  .pause-overlay {
    border-radius: 18px;
  }

  .pause-panel {
    width: min(92%, 420px);
    padding: 26px 20px;
  }

  .pause-title {
    font-size: 1.15rem;
  }

  .battle-area :deep(.chat-box) {
    width: 100%;
    min-height: 220px;
    height: clamp(220px, 30vh, 320px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .pause-overlay {
    animation: none;
  }
}
</style>
