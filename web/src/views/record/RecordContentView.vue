<template>
  <section class="record-content-wrap">
    <div class="record-content-head">
      <h2>Match Replay</h2>
      <p>Playback of a historical battle record.</p>
    </div>
    <PlayGround />
  </section>
</template>

<script setup>
import PlayGround from "@/components/PlayGround.vue";
</script>

<style scoped>
.record-content-wrap {
  width: min(1080px, 96vw);
  margin: 20px auto 0;
  animation: reveal 260ms ease;
}

.record-content-head {
  text-align: center;
  margin-bottom: 10px;
}

.record-content-head h2 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
}

.record-content-head p {
  margin: 6px 0 0;
  color: #a9bfd3;
}

@keyframes reveal {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
