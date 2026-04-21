<template>
  <div class="result-board">
    <div class="result-board-text" v-if="pkStore.loser === 'all'">平局</div>
    <div class="result-board-text" v-else-if="isLose">你输了</div>
    <div class="result-board-text" v-else>你赢了</div>
    <div class="result-board-btn">
      <button @click="restart" type="button" class="btn btn-warning btn-lg">再来一局</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import defaultAvatar from "@/assets/images/default-avatar.svg";

const userStore = useUserStore();
const pkStore = usePkStore();

const isLose = computed(() => {
  const userId = Number.parseInt(userStore.id, 10);
  return (
    (pkStore.loser === "A" && Number.parseInt(pkStore.a_id, 10) === userId) ||
    (pkStore.loser === "B" && Number.parseInt(pkStore.b_id, 10) === userId)
  );
});

/**
 * Handles restart.
 * ??restart?
 */
const restart = () => {
  pkStore.updateRoomId("");
  pkStore.updateStatus("matching");
  pkStore.updateLoser("none");
  pkStore.updateResultVisible(false);
  pkStore.updateOpponent({
    username: "匹配对手",
    photo: defaultAvatar,
  });
};
</script>

<style scoped>
div.result-board {
  height: 30vh;
  width: min(520px, 76vw);
  border-radius: 20px;
  border: 1px solid var(--kob-panel-border);
  background: var(--kob-panel);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 44px rgba(0, 50, 100, 0.15);
  position: absolute;
  top: 30vh;
  left: 50%;
  transform: translateX(-50%);
  animation: board-enter 220ms ease;
}

div.result-board-text {
  text-align: center;
  color: var(--kob-text);
  font-size: 50px;
  font-weight: 700;
  font-style: italic;
  padding-top: 5vh;
  font-family: "Space Grotesk", sans-serif;
  text-shadow: 0 2px 10px rgba(61, 174, 255, 0.2);
}

div.result-board-btn {
  padding-top: 7vh;
  text-align: center;
}

.result-board-btn .btn {
  border: 0;
  border-radius: 999px;
  padding: 10px 28px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  box-shadow: 0 6px 16px rgba(61, 174, 255, 0.3);
  font-weight: 600;
  transition: transform 180ms ease;
}

.result-board-btn .btn:hover {
  transform: translateY(-2px);
}

@keyframes board-enter {
  from {
    opacity: 0;
    transform: translateX(-50%) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
}
</style>
