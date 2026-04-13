<template>
  <PlayGround v-if="pkStore.status === 'playing'" />
  <MatchGround v-if="pkStore.status === 'matching'" />
  <ResultBoard v-if="pkStore.loser !== 'none'" />
  <div class="user-color" v-if="isAPlayer" @mousedown.stop>Bottom Left</div>
  <div class="user-color" v-if="isBPlayer" @mousedown.stop>Top Right</div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from "vue";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import MatchGround from "@/components/MatchGround.vue";
import PlayGround from "@/components/PlayGround.vue";
import ResultBoard from "@/components/ResultBoard.vue";
import { buildWebSocketUrl } from "@/config/env";

const userStore = useUserStore();
const pkStore = usePkStore();
const recordStore = useRecordStore();

let socket = null;

const currentUserId = computed(() => Number.parseInt(userStore.id, 10));
const isAPlayer = computed(
  () => pkStore.status === "playing" && currentUserId.value === Number.parseInt(pkStore.a_id, 10)
);
const isBPlayer = computed(
  () => pkStore.status === "playing" && currentUserId.value === Number.parseInt(pkStore.b_id, 10)
);

onMounted(() => {
  pkStore.updateLoser("none");
  recordStore.updateIsRecord(false);
  pkStore.updatePaused({ isPaused: false, pausedByUserId: null });
  pkStore.updateOpponent({
    username: "My Opponent",
    photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
  });

  const token = userStore.token;
  if (!token) return;

  socket = new WebSocket(buildWebSocketUrl(token));

  socket.onopen = () => {
    pkStore.updateSocket(socket);
  };

  socket.onmessage = (msg) => {
    const data = JSON.parse(msg.data);

    if (data.event === "start-matching") {
      pkStore.updateOpponent({
        username: data.opponent_username,
        photo: data.opponent_photo,
      });
      pkStore.updateRoomId(data.room_id || "");
      setTimeout(() => {
        pkStore.updateStatus("playing");
      }, 200);
      pkStore.updateGame(data.game);
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
      if (!game) return;
      const [snake0, snake1] = game.snakes;
      if (data.loser === "all" || data.loser === "A") snake0.status = "die";
      if (data.loser === "all" || data.loser === "B") snake1.status = "die";
      pkStore.updateLoser(data.loser);
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
  };

  socket.onclose = () => {
    pkStore.updateSocket(null);
  };
});

onUnmounted(() => {
  try {
    if (socket) {
      socket.onopen = null;
      socket.onmessage = null;
      socket.onclose = null;
      if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
        socket.close();
      }
    }
  } catch (error) {
    console.error("pk socket cleanup error:", error);
  } finally {
    socket = null;
    pkStore.updateSocket(null);
    pkStore.updateLoser("none");
    pkStore.updateStatus("matching");
    pkStore.updateRoomId("");
    pkStore.updatePaused({ isPaused: false, pausedByUserId: null });
  }
});
</script>

<style scoped>
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
</style>

