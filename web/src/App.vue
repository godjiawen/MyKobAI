<template>
  <div class="app-shell">
    <div class="bg-layer bg-gradient"></div>
    <div class="bg-layer bg-grid"></div>
    <Navbar />
    <main class="app-main">
      <section v-if="renderError" class="app-error-panel container">
        <h3>页面渲染异常</h3>
        <p>{{ renderError }}</p>
        <button class="btn btn-primary" type="button" @click="resetRenderError">清除错误并继续</button>
      </section>

      <router-view v-else v-slot="{ Component, route }">
        <transition name="route-fade" mode="out-in" appear>
          <keep-alive v-if="route.meta.keepAlive" include="RecordIndexView,RanklistIndexView">
            <component :is="Component" :key="route.name" />
          </keep-alive>
        </transition>

        <transition name="route-fade" mode="out-in" appear>
          <component
            v-if="!route.meta.keepAlive"
            :is="Component"
            :key="route.fullPath"
          />
        </transition>
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
  --kob-bg: #f5f9ff;
  --kob-bg-soft: #eaf3fb;
  --kob-panel: rgba(255, 255, 255, 0.85);
  --kob-panel-border: rgba(90, 180, 255, 0.3);
  --kob-text: #2c3e50;
  --kob-muted: #6b839b;
  --kob-accent: #3daeff;
  --kob-accent-strong: #0088e8;
  --kob-warn: #ffb732;

  --motion-fast: 140ms;
  --motion-medium: 260ms;
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
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
    radial-gradient(900px 500px at 12% 6%, rgba(135, 206, 250, 0.4), transparent 60%),
    radial-gradient(800px 450px at 88% 84%, rgba(255, 218, 117, 0.35), transparent 62%),
    linear-gradient(180deg, #f0f7ff 0%, #e2f0fc 100%);
  z-index: -3;
}

.bg-grid {
  background-image:
    linear-gradient(rgba(90, 180, 255, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(90, 180, 255, 0.15) 1px, transparent 1px);
  background-size: 38px 38px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
  z-index: -2;
}

.app-main {
  position: relative;
  padding-bottom: 28px;
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition:
    opacity var(--motion-medium) var(--ease-out),
    transform var(--motion-medium) var(--ease-out);
}

.route-fade-enter-from,
.route-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.route-fade-enter-to,
.route-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.app-error-panel {
  margin-top: 20px;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid rgba(255, 143, 143, 0.45);
  background: rgba(255, 230, 230, 0.8);
}

.app-error-panel h3 {
  margin: 0 0 8px;
  font-size: 1.1rem;
  color: #c0392b;
}

.app-error-panel p {
  margin: 0 0 12px;
  color: #e74c3c;
}

.modal-backdrop {
  z-index: 3000 !important;
}

.modal {
  z-index: 3010 !important;
}

.content-field .card {
  background: var(--kob-panel);
  border: 1px solid var(--kob-panel-border);
  border-radius: 18px;
  box-shadow: 0 14px 40px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(8px);
}

.content-field .card-body {
  color: var(--kob-text);
}

.content-field .table {
  color: var(--kob-text);
  --bs-table-bg: transparent;
  --bs-table-striped-bg: rgba(0, 0, 0, 0.02);
  --bs-table-hover-bg: rgba(90, 180, 255, 0.1);
  border-color: rgba(90, 180, 255, 0.2);
}

.content-field .form-control,
.content-field .form-select {
  border-radius: 12px;
  border: 1px solid rgba(90, 180, 255, 0.3);
  background: #ffffff;
  color: var(--kob-text);
}

.content-field .form-control:focus,
.content-field .form-select:focus {
  box-shadow: 0 0 0 0.2rem rgba(90, 180, 255, 0.22);
  border-color: rgba(90, 180, 255, 0.45);
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
  border-color: rgba(90, 180, 255, 0.3);
  background: #ffffff;
  color: var(--kob-text);
}

.content-field .pagination .page-item.active .page-link {
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
  border-color: transparent;
  color: #ffffff;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }

  .route-fade-enter-from,
  .route-fade-leave-to,
  .route-fade-enter-to,
  .route-fade-leave-from {
    transform: none !important;
  }
}
</style>
