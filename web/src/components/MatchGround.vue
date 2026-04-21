<template>
  <section class="match-shell kob-page-shell">
    <div class="matchground kob-surface">
      <header class="match-header">
        <div>
          <p class="kob-kicker">Match Center</p>
          <h3 class="kob-headline">实时匹配</h3>
          <p class="kob-subtitle">选择出战 Bot，开始匹配或取消匹配。</p>
        </div>
      </header>

      <div class="match-grid">
        <article class="player-card">
          <img :src="userStore.photo" alt="" class="player-avatar" />
          <strong>{{ userStore.username }}</strong>
          <span>当前玩家</span>
        </article>

        <article class="control-card">
          <label class="control-label" for="bot-select">出战设置</label>
          <select id="bot-select" v-model="selectedBot" class="form-select" aria-label="选择出战 Bot">
            <option value="-1">手动操作</option>
            <option v-for="bot in bots" :key="bot.id" :value="String(bot.id)">
              {{ bot.title }}
            </option>
          </select>
          <button
            type="button"
            class="btn btn-primary btn-lg match-action-btn kob-pill-btn"
            :disabled="isButtonDisabled"
            @click="toggleMatch"
          >
            {{ matchButtonText }}
          </button>
          <p v-if="stateHint" class="match-state-hint">{{ stateHint }}</p>
        </article>

        <article class="player-card">
          <img :src="pkStore.opponent_photo" alt="" class="player-avatar" />
          <strong>{{ pkStore.opponent_username }}</strong>
          <span>待匹配对手</span>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const MATCH_STATES = {
  IDLE: "idle",
  MATCHING: "matching",
  MATCHED: "matched",
  ERROR: "error",
};

const userStore = useUserStore();
const pkStore = usePkStore();

const bots = ref([]);
const selectedBot = ref("-1");
const matchState = ref(MATCH_STATES.IDLE);

const isSocketReady = computed(
  () => pkStore.socket && pkStore.socket.readyState === WebSocket.OPEN
);

const matchButtonText = computed(() => {
  if (matchState.value === MATCH_STATES.MATCHING) return "取消匹配";
  if (matchState.value === MATCH_STATES.MATCHED) return "匹配成功";
  if (matchState.value === MATCH_STATES.ERROR) return "连接异常，重试匹配";
  return "开始匹配";
});

const stateHint = computed(() => {
  if (matchState.value === MATCH_STATES.MATCHING) return "正在为你寻找对手...";
  if (matchState.value === MATCH_STATES.ERROR) return "连接已断开，请等待自动重连后重试。";
  return "";
});

const isButtonDisabled = computed(() => {
  if (matchState.value === MATCH_STATES.MATCHED) return true;
  if (matchState.value === MATCH_STATES.IDLE || matchState.value === MATCH_STATES.ERROR) {
    return !isSocketReady.value;
  }
  return false;
});

const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, {
      token: userStore.token,
    });
  } catch (error) {
    console.error(error);
  }
};

/**
 * Handles sendStartMatching.
 * ??sendStartMatching?
 */
const sendStartMatching = () => {
  const socket = pkStore.socket;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    matchState.value = MATCH_STATES.ERROR;
    return;
  }

  socket.send(
    JSON.stringify({
      event: "start-matching",
      bot_id: Number.parseInt(selectedBot.value, 10),
    })
  );
  matchState.value = MATCH_STATES.MATCHING;
};

/**
 * Handles sendStopMatching.
 * ??sendStopMatching?
 */
const sendStopMatching = () => {
  const socket = pkStore.socket;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    matchState.value = MATCH_STATES.ERROR;
    return;
  }

  socket.send(JSON.stringify({ event: "stop-matching" }));
  matchState.value = MATCH_STATES.IDLE;
};

/**
 * Handles toggleMatch.
 * ??toggleMatch?
 */
const toggleMatch = () => {
  if (matchState.value === MATCH_STATES.MATCHING) {
    sendStopMatching();
    return;
  }

  if (matchState.value === MATCH_STATES.MATCHED) return;
  sendStartMatching();
};

watch(
  () => pkStore.status,
  (status) => {
    if (status === "playing") {
      matchState.value = MATCH_STATES.MATCHED;
      return;
    }

    if (status === "matching" && matchState.value === MATCH_STATES.MATCHED) {
      matchState.value = MATCH_STATES.IDLE;
    }
  },
  { immediate: true }
);

watch(
  () => pkStore.socket,
  (socket) => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      if (matchState.value === MATCH_STATES.MATCHING) {
        matchState.value = MATCH_STATES.ERROR;
      }
      return;
    }

    if (matchState.value === MATCH_STATES.ERROR) {
      matchState.value = MATCH_STATES.IDLE;
    }
  }
);

watch(
  selectedBot,
  (value) => {
    pkStore.updateSelectedBot(value);
  },
  { immediate: true }
);

onMounted(() => {
  selectedBot.value = pkStore.selectedBotId || "-1";
  refreshBots();
});
</script>

<style scoped>
.matchground {
  padding: 24px;
}

.match-header {
  margin-bottom: 18px;
}

.match-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.player-card,
.control-card {
  border-radius: var(--kob-radius-lg);
  border: 1px solid rgba(90, 180, 255, 0.18);
  background: rgba(255, 255, 255, 0.8);
  box-shadow: var(--kob-shadow-sm);
}

.player-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px;
}

.player-avatar {
  width: clamp(96px, 12vw, 140px);
  height: clamp(96px, 12vw, 140px);
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 0 20px rgba(90, 180, 255, 0.35);
  object-fit: cover;
}

.player-card strong {
  color: var(--kob-text);
  font-size: 1.05rem;
}

.player-card span {
  color: var(--kob-muted);
  font-size: 0.84rem;
}

.control-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.control-label {
  color: var(--kob-muted);
  font-size: 0.84rem;
  font-weight: 700;
}

.match-action-btn {
  margin-top: 6px;
}

.match-state-hint {
  margin: 0;
  color: var(--kob-muted);
  font-size: 0.88rem;
}

@media (max-width: 991px) {
  .matchground {
    padding: 18px;
  }

  .match-grid {
    grid-template-columns: 1fr;
  }

  .player-card {
    flex-direction: row;
    justify-content: flex-start;
    gap: 14px;
  }

  .player-avatar {
    width: 80px;
    height: 80px;
  }
}
</style>
