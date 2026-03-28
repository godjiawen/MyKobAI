<template>
  <PlayGround v-if="store.state.pk.status === 'playing'" />
  <MatchGround v-if="store.state.pk.status === 'matching'" />
  <ResultBoard v-if="store.state.pk.loser !== 'none'" />
  <div class="user-color" v-if="isAPlayer">Bottom Left</div>
  <div class="user-color" v-if="isBPlayer">Top Right</div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from "vue";
import { useStore } from "vuex";
import MatchGround from "@/components/MatchGround.vue";
import PlayGround from "@/components/PlayGround.vue";
import ResultBoard from "@/components/ResultBoard.vue";
import { buildWebSocketUrl } from "@/config/env";

const store = useStore();

let socket = null;

const currentUserId = computed(() => Number.parseInt(store.state.user.id, 10));
const isAPlayer = computed(
  () => store.state.pk.status === "playing" && currentUserId.value === Number.parseInt(store.state.pk.a_id, 10)
);
const isBPlayer = computed(
  () => store.state.pk.status === "playing" && currentUserId.value === Number.parseInt(store.state.pk.b_id, 10)
);

onMounted(() => {
  store.commit("updateLoser", "none");
  store.commit("updateIsRecord", false);
  store.commit("updateOpponent", {
    username: "My Opponent",
    photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
  });

  const token = store.state.user.token;
  if (!token) return;

  socket = new WebSocket(buildWebSocketUrl(token));

  socket.onopen = () => {
    store.commit("updateSocket", socket);
  };

  socket.onmessage = (msg) => {
    const data = JSON.parse(msg.data);
    if (data.event === "start-matching") {
      store.commit("updateOpponent", {
        username: data.opponent_username,
        photo: data.opponent_photo,
      });
      setTimeout(() => {
        store.commit("updateStatus", "playing");
      }, 200);
      store.commit("updateGame", data.game);
      return;
    }

    if (data.event === "move") {
      const game = store.state.pk.gameObject;
      if (!game) return;
      const [snake0, snake1] = game.snakes;
      snake0.set_direction(data.a_direction);
      snake1.set_direction(data.b_direction);
      return;
    }

    if (data.event === "result") {
      const game = store.state.pk.gameObject;
      if (!game) return;
      const [snake0, snake1] = game.snakes;

      if (data.loser === "all" || data.loser === "A") {
        snake0.status = "die";
      }
      if (data.loser === "all" || data.loser === "B") {
        snake1.status = "die";
      }
      store.commit("updateLoser", data.loser);
    }
  };

  socket.onclose = () => {
    store.commit("updateSocket", null);
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
    store.commit("updateSocket", null);
    store.commit("updateLoser", "none");
    store.commit("updateStatus", "matching");
  }
});
</script>

<style scoped>
div.user-color {
  text-align: center;
  color: #f2f8ff;
  font-size: 24px;
  font-weight: 600;
  width: fit-content;
  margin: 12px auto 0;
  border: 1px solid rgba(145, 210, 255, 0.3);
  border-radius: 999px;
  padding: 6px 16px;
  background: rgba(8, 26, 40, 0.7);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.22);
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
