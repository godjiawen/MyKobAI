<template>
  <div class="matchground">
    <div class="row">
      <div class="col-4">
        <div class="user-photo">
          <img :src="store.state.user.photo" alt="" />
        </div>
        <div class="user-username">{{ store.state.user.username }}</div>
      </div>
      <div class="col-4">
        <div class="user-select-bot">
          <select v-model="selectedBot" class="form-select" aria-label="Default select example">
            <option value="-1">Manual Play</option>
            <option v-for="bot in bots" :key="bot.id" :value="String(bot.id)">
              {{ bot.title }}
            </option>
          </select>
        </div>
      </div>
      <div class="col-4">
        <div class="user-photo">
          <img :src="store.state.pk.opponent_photo" alt="" />
        </div>
        <div class="user-username">{{ store.state.pk.opponent_username }}</div>
      </div>
      <div class="col-12" style="text-align: center; padding-top: 15vh;">
        <button @click="toggleMatch" type="button" class="btn btn-warning btn-lg">
          {{ matchButtonText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useStore } from "vuex";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const store = useStore();
const bots = ref([]);
const selectedBot = ref("-1");
const matchButtonText = ref("Start Matching");

const refreshBots = async () => {
  try {
    bots.value = await apiRequest(API_PATHS.botList, {
      token: store.state.user.token,
    });
  } catch (error) {
    console.error(error);
  }
};

const toggleMatch = () => {
  const socket = store.state.pk.socket;
  if (!socket) return;

  if (matchButtonText.value === "Start Matching") {
    matchButtonText.value = "Cancel";
    socket.send(
      JSON.stringify({
        event: "start-matching",
        bot_id: Number.parseInt(selectedBot.value, 10),
      })
    );
  } else {
    matchButtonText.value = "Start Matching";
    socket.send(JSON.stringify({ event: "stop-matching" }));
  }
};

onMounted(refreshBots);
</script>

<style scoped>
div.matchground {
  width: min(980px, 92vw);
  height: 70vh;
  margin: 34px auto;
  border-radius: 24px;
  border: 1px solid var(--kob-panel-border);
  background: var(--kob-panel);
  box-shadow: 0 24px 50px rgba(0, 50, 100, 0.08); /* light shadow */
  backdrop-filter: blur(12px);
  animation: panel-enter 280ms ease;
}

div.user-photo {
  text-align: center;
  padding-top: 10vh;
}

div.user-photo > img {
  border-radius: 50%;
  width: 20vh;
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
  padding-top: 20vh;
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

.btn.btn-warning:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(61, 174, 255, 0.4);
}

@keyframes panel-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
