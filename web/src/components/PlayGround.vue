<template>
  <div class="playground">
    <GameMap />
  </div>
</template>

<script setup>
import GameMap from "@/components/GameMap.vue";
</script>

<style scoped>
div.playground {
  width: min(1000px, 92vw);
  height: clamp(420px, 70vh, 760px);
  margin: 22px auto 40px;
  border: 1px solid var(--kob-panel-border);
  border-radius: 22px;
  background: var(--kob-panel);
  box-shadow: 0 22px 48px rgba(0, 50, 100, 0.08); /* light shadow */
  backdrop-filter: blur(12px);
  padding: 14px;
}
</style>
