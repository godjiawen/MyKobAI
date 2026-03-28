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
  border: 1px solid rgba(145, 210, 255, 0.24);
  border-radius: 22px;
  background: linear-gradient(165deg, rgba(8, 26, 40, 0.86), rgba(8, 20, 34, 0.72));
  box-shadow: 0 22px 48px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  padding: 14px;
}
</style>
