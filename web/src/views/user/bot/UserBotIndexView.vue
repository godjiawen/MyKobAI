<template>
  <section class="kob-page-shell bot-page">
    <div class="bot-layout">
      <ProfileCard />
      <BotList />
    </div>
  </section>
</template>

<script setup>
import ProfileCard from "./components/ProfileCard.vue";
import BotList from "./components/BotList.vue";
</script>

<style scoped>
.bot-layout {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

@media (max-width: 991px) {
  .bot-layout {
    grid-template-columns: 1fr;
  }
}
</style>
