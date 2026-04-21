<!-- 界面组件。 -->
<template>
  <div ref="parent" class="gamemap">
    <canvas ref="canvas" tabindex="0"></canvas>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import { GameMap } from "@/assets/scripts/GameMap";
import { startGameLoop, stopGameLoop } from "@/assets/scripts/AcGameObject";

const pkStore = usePkStore();
const recordStore = useRecordStore();
const parent = ref(null);
const canvas = ref(null);

onMounted(() => {
  const ctx = canvas.value?.getContext("2d");
  if (!ctx || !parent.value) return;

  pkStore.updateGameObject(new GameMap(ctx, parent.value, { pkStore, recordStore }));
  startGameLoop();
});

onUnmounted(() => {
  try {
    const gameObject = pkStore.gameObject;
    if (gameObject && typeof gameObject.destroy === "function") {
      gameObject.destroy();
    }
  } catch (error) {
    console.error("game map cleanup error:", error);
  } finally {
    stopGameLoop();
    pkStore.updateGameObject(null);
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
  border-radius: var(--kob-radius-sm);
  background:
    radial-gradient(500px 200px at 50% -10%, rgba(90, 180, 255, 0.18), transparent 70%),
    linear-gradient(180deg, rgba(220, 235, 250, 0.6), rgba(233, 243, 252, 0.58));
}

canvas {
  border-radius: 12px;
  outline: 1px solid rgba(90, 180, 255, 0.3);
  box-shadow: inset 0 0 0 1px rgba(90, 180, 255, 0.15), 0 14px 26px rgba(0, 50, 100, 0.12);
}
</style>
