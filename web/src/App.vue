<template>
  <div class="app-shell">
    <div class="bg-layer bg-gradient"></div>
    <div class="bg-layer bg-grid"></div>
    <Navbar />
    <main class="app-main">
      <section v-if="renderError" class="app-error-panel container">
        <h3>页面渲染异常</h3>
        <p>{{ renderError }}</p>
        <button class="btn btn-primary" type="button" @click="resetRenderError">
          清除错误并继续
        </button>
      </section>
      <router-view v-else v-slot="{ Component, route }">
        <component :is="Component" :key="route.fullPath" />
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { onErrorCaptured, ref } from "vue";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import Navbar from "@/components/NavBar.vue";

const renderError = ref("");

const resetRenderError = () => {
  renderError.value = "";
};

onErrorCaptured((error) => {
  console.error(error);
  renderError.value = error?.message || "Unknown render error";
  return false;
});
</script>

<style>
@import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&family=IBM+Plex+Sans:wght@400;500;600&display=swap");

:root {
  --kob-bg: #08141f;
  --kob-bg-soft: #102338;
  --kob-panel: rgba(7, 22, 36, 0.72);
  --kob-panel-border: rgba(145, 210, 255, 0.2);
  --kob-text: #e8f4ff;
  --kob-muted: #a9bfd3;
  --kob-accent: #5ad1ff;
  --kob-accent-strong: #23b0ff;
  --kob-warn: #ffbf47;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
  color: var(--kob-text);
  background: var(--kob-bg);
  font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
}

.app-shell {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

.bg-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.bg-gradient {
  background:
    radial-gradient(900px 500px at 12% 6%, rgba(39, 186, 255, 0.22), transparent 60%),
    radial-gradient(800px 450px at 88% 84%, rgba(255, 191, 71, 0.18), transparent 62%),
    linear-gradient(180deg, #0a1726 0%, #070f18 100%);
  z-index: -3;
}

.bg-grid {
  background-image:
    linear-gradient(rgba(90, 209, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(90, 209, 255, 0.07) 1px, transparent 1px);
  background-size: 38px 38px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
  z-index: -2;
}

.app-main {
  position: relative;
  padding-bottom: 28px;
}

.app-error-panel {
  margin-top: 20px;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid rgba(255, 143, 143, 0.45);
  background: rgba(55, 16, 22, 0.5);
}

.app-error-panel h3 {
  margin: 0 0 8px;
  font-size: 1.1rem;
}

.app-error-panel p {
  margin: 0 0 12px;
  color: #ffd7d7;
}

.content-field .card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  border-radius: 18px;
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
}

.content-field .card-body {
  color: var(--kob-text);
}

.content-field .table {
  color: var(--kob-text);
  --bs-table-bg: transparent;
  --bs-table-striped-bg: rgba(255, 255, 255, 0.04);
  --bs-table-hover-bg: rgba(90, 209, 255, 0.08);
  border-color: rgba(170, 205, 232, 0.16);
}

.content-field .form-control,
.content-field .form-select {
  border-radius: 12px;
  border: 1px solid rgba(139, 198, 237, 0.22);
  background: rgba(12, 26, 42, 0.82);
  color: var(--kob-text);
}

.content-field .form-control:focus,
.content-field .form-select:focus {
  box-shadow: 0 0 0 0.2rem rgba(90, 209, 255, 0.22);
  border-color: rgba(90, 209, 255, 0.45);
}

.content-field .btn-primary {
  border: 0;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
}

.content-field .btn-warning {
  border: 0;
  color: #2c1f00;
  background: linear-gradient(135deg, #ffd05d, var(--kob-warn));
}

.content-field .pagination .page-link {
  border-color: rgba(90, 209, 255, 0.24);
  background: rgba(10, 31, 48, 0.76);
  color: var(--kob-text);
}

.content-field .pagination .page-item.active .page-link {
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  border-color: transparent;
}

</style>
