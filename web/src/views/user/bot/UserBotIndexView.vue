<template>
  <div class="container bot-page">
    <div class="row g-3">
      <div class="col-lg-3 col-md-4">
        <ProfileCard />
      </div>
      <div class="col-lg-9 col-md-8">
        <BotList />
      </div>
    </div>
  </div>
</template>

<script setup>
import ProfileCard from "./components/ProfileCard.vue";
import BotList from "./components/BotList.vue";
</script>

<style scoped>
.bot-page {
  margin-top: 20px;
}
</style>
