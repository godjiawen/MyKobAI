<template>
  <div class="result-board">
    <div class="result-board-text" v-if="store.state.pk.loser === 'all'">Draw!</div>
    <div class="result-board-text" v-else-if="isLose">You Lose!</div>
    <div class="result-board-text" v-else>You Win!</div>
    <div class="result-board-btn">
      <button @click="restart" type="button" class="btn btn-warning btn-lg">Again!</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useStore } from "vuex";

const store = useStore();

const isLose = computed(() => {
  const userId = Number.parseInt(store.state.user.id, 10);
  return (
    (store.state.pk.loser === "A" && store.state.pk.a_id === userId) ||
    (store.state.pk.loser === "B" && store.state.pk.b_id === userId)
  );
});

const restart = () => {
  store.commit("updateStatus", "matching");
  store.commit("updateLoser", "none");
  store.commit("updateOpponent", {
    username: "My Opponent",
    photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
  });
};
</script>

<style scoped>
div.result-board {
  height: 30vh;
  width: min(520px, 76vw);
  border-radius: 20px;
  border: 1px solid rgba(145, 210, 255, 0.26);
  background: linear-gradient(170deg, rgba(8, 30, 44, 0.93), rgba(8, 20, 34, 0.8));
  backdrop-filter: blur(8px);
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.34);
  position: absolute;
  top: 30vh;
  left: 50%;
  transform: translateX(-50%);
  animation: board-enter 220ms ease;
}

div.result-board-text {
  text-align: center;
  color: #f3f9ff;
  font-size: 50px;
  font-weight: 600;
  font-style: italic;
  padding-top: 5vh;
  font-family: "Space Grotesk", sans-serif;
}

div.result-board-btn {
  padding-top: 7vh;
  text-align: center;
}

.result-board-btn .btn {
  border: 0;
  border-radius: 999px;
  padding: 10px 28px;
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
