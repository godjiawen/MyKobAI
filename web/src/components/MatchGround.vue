<template>
  <div class="matchground">
    <div class="row">
      <div class="col-4">
        <div class="user-photo">
          <img :src="userStore.photo" alt="" />
        </div>
        <div class="user-username">{{ userStore.username }}</div>
      </div>
      <div class="col-4">
        <div class="user-select-bot">
          <select v-model="selectedBot" class="form-select" aria-label="选择出战 Bot">
            <option value="-1">手动操作</option>
            <option v-for="bot in bots" :key="bot.id" :value="String(bot.id)">
              {{ bot.title }}
            </option>
          </select>
        </div>
      </div>
      <div class="col-4">
        <div class="user-photo">
          <img :src="pkStore.opponent_photo" alt="" />
        </div>
        <div class="user-username">{{ pkStore.opponent_username }}</div>
      </div>
      <div class="col-12 match-action-row">
        <button
          type="button"
          class="btn btn-warning btn-lg match-action-btn"
          :disabled="isButtonDisabled"
          @click="toggleMatch"
        >
          {{ matchButtonText }}
        </button>
        <p v-if="stateHint" class="match-state-hint">{{ stateHint }}</p>
      </div>
    </div>
  </div>
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

const sendStopMatching = () => {
  const socket = pkStore.socket;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    matchState.value = MATCH_STATES.ERROR;
    return;
  }

  socket.send(JSON.stringify({ event: "stop-matching" }));
  matchState.value = MATCH_STATES.IDLE;
};

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
div.matchground {
  width: min(980px, 92vw);
  min-height: 540px;
  height: min(70vh, 740px);
  margin: 34px auto;
  border-radius: 24px;
  border: 1px solid var(--kob-panel-border);
  background: var(--kob-panel);
  box-shadow: 0 24px 50px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(12px);
  animation: panel-enter 280ms ease;
}

div.user-photo {
  text-align: center;
  padding-top: clamp(40px, 10vh, 96px);
}

div.user-photo > img {
  border-radius: 50%;
  width: clamp(120px, 20vh, 200px);
  border: 4px solid #ffffff;
  box-shadow: 0 0 24px rgba(90, 180, 255, 0.4);
}

div.user-username {
  text-align: center;
  font-size: 24px;
  font-weight: 600;
  color: var(--kob-text);
  padding-top: 2vh;
}

div.user-select-bot {
  padding-top: clamp(110px, 20vh, 180px);
}

div.user-select-bot > select {
  width: 80%;
  margin: 0 auto;
  border-radius: 999px;
  background-color: #ffffff;
  border: 1px solid rgba(90, 180, 255, 0.4);
  color: var(--kob-text);
}

.btn.btn-warning {
  border-radius: 999px;
  padding: 11px 36px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  border: none;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.match-action-row {
  text-align: center;
  padding-top: clamp(26px, 15vh, 120px);
}

.match-action-btn {
  min-width: 190px;
}

.match-action-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.match-state-hint {
  margin: 10px 0 0;
  color: var(--kob-muted);
  font-size: 0.9rem;
}

.btn.btn-warning:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(61, 174, 255, 0.4);
}

@keyframes panel-enter {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 991px) {
  div.matchground {
    width: min(94vw, 680px);
    min-height: 0;
    height: auto;
    padding: 12px 8px 18px;
    margin: 16px auto 24px;
  }

  div.user-photo {
    padding-top: 20px;
  }

  div.user-photo > img {
    width: clamp(88px, 22vw, 128px);
    border-width: 3px;
  }

  div.user-username {
    font-size: 18px;
    padding-top: 10px;
  }

  div.user-select-bot {
    padding-top: 24px;
  }

  div.user-select-bot > select {
    width: min(320px, 90%);
  }

  .match-action-row {
    padding-top: 20px;
  }
}
</style>
