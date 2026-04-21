<template>
  <ContentField>
    <section class="notfound-wrap">
      <p class="code">404</p>
      <h1>页面在竞技场里迷路了</h1>
      <p class="desc">你访问的页面不存在，或已被移动到其他位置。</p>
      <router-link class="btn btn-primary back-btn kob-pill-btn" :to="{ name: 'home' }">返回首页</router-link>
    </section>
  </ContentField>
</template>

<script setup>
import ContentField from "@/components/ContentField.vue";
</script>

<style scoped>
.notfound-wrap {
  padding: 26px 10px;
  text-align: center;
}

.code {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  font-size: clamp(52px, 9vw, 92px);
  line-height: 1;
  background: linear-gradient(135deg, #5ad1ff, #ffd05d);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h1 {
  margin: 10px 0 6px;
  font-family: "Space Grotesk", sans-serif;
  font-size: clamp(24px, 4vw, 34px);
  color: var(--kob-text);
}

.desc {
  margin: 0 auto 18px;
  max-width: 580px;
  color: var(--kob-muted);
}

.back-btn {
  padding-inline: 22px;
}
</style>
