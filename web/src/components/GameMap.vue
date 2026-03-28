<template>
  <div ref="parent" class="gamemap">
    <canvas ref="canvas" tabindex="0"></canvas>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useStore } from "vuex";
import { GameMap } from "@/assets/scripts/GameMap";

const store = useStore();
const parent = ref(null);
const canvas = ref(null);

onMounted(() => {
  const ctx = canvas.value?.getContext("2d");
  if (!ctx || !parent.value) return;

  store.commit("updateGameObject", new GameMap(ctx, parent.value, store));
});

onUnmounted(() => {
  try {
    const gameObject = store.state.pk.gameObject;
    if (gameObject && typeof gameObject.destroy === "function") {
      gameObject.destroy();
    }
  } catch (error) {
    console.error("game map cleanup error:", error);
  } finally {
    store.commit("updateGameObject", null);
  }
});
</script>

<style scoped>
div.gamemap {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 16px;
  background:
    radial-gradient(500px 200px at 50% -10%, rgba(90, 209, 255, 0.08), transparent 70%),
    rgba(6, 17, 29, 0.54);
}

canvas {
  border-radius: 10px;
  outline: 1px solid rgba(145, 210, 255, 0.25);
  box-shadow: inset 0 0 0 1px rgba(145, 210, 255, 0.08), 0 14px 26px rgba(0, 0, 0, 0.28);
}
</style>
