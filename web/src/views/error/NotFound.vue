<template>
  <ContentField>
    <section class="notfound-wrap">
      <p class="code">404</p>
      <h1>Page Lost In The Arena</h1>
      <p class="desc">The page you requested does not exist or has been moved.</p>
      <router-link class="btn btn-primary back-btn" :to="{ name: 'home' }">Back Home</router-link>
    </section>
  </ContentField>
</template>

<script setup>
import ContentField from "@/components/ContentField.vue";
</script>

<style scoped>
.notfound-wrap {
  padding: 22px 10px;
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
}

.desc {
  margin: 0 auto 18px;
  max-width: 580px;
  color: #a9bfd3;
}

.back-btn {
  border-radius: 999px;
  padding-inline: 22px;
}
</style>
