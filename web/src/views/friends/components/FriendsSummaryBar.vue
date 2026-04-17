<template>
  <div class="friends-summary-grid">
    <article
      v-for="(card, index) in displayCards"
      :key="card.label || `summary-loading-${index}`"
      class="summary-card"
      :class="{ 'is-loading': loading }"
      :style="{ '--stagger-delay': `${index * 45}ms` }"
    >
      <template v-if="loading">
        <span class="summary-line shimmer-line summary-line--label"></span>
        <span class="summary-line shimmer-line summary-line--value"></span>
        <span class="summary-line shimmer-line summary-line--footnote"></span>
      </template>
      <template v-else>
        <span class="summary-label">{{ card.label }}</span>
        <strong class="summary-value">{{ card.value }}</strong>
        <small class="summary-footnote">{{ card.footnote }}</small>
      </template>
    </article>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  cards: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
});

const displayCards = computed(() => {
  if (props.loading) {
    return Array.from({ length: 4 }, (_, index) => ({ label: `loading-${index}` }));
  }
  return props.cards;
});
</script>

<style scoped>
.friends-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(90, 180, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 249, 255, 0.8)),
    radial-gradient(circle at top right, rgba(255, 208, 93, 0.12), transparent 36%);
  box-shadow: 0 14px 34px rgba(0, 50, 100, 0.08);
  animation: rise-in 380ms var(--ease-out) both;
  animation-delay: var(--stagger-delay);
}

.summary-card:nth-child(1) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(237, 248, 255, 0.84)),
    radial-gradient(circle at top right, rgba(61, 174, 255, 0.14), transparent 34%);
}

.summary-card:nth-child(2) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(240, 255, 246, 0.82)),
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.12), transparent 34%);
}

.summary-card:nth-child(3) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 249, 238, 0.84)),
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.14), transparent 34%);
}

.summary-card:nth-child(4) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(249, 245, 255, 0.84)),
    radial-gradient(circle at top right, rgba(124, 58, 237, 0.12), transparent 34%);
}

.summary-label {
  display: block;
  color: var(--kob-muted);
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summary-value {
  display: block;
  margin-top: 10px;
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.85rem;
  line-height: 1;
  color: var(--kob-text);
}

.summary-footnote {
  display: block;
  margin-top: 8px;
  color: var(--kob-muted);
  font-size: 0.82rem;
}

.summary-line {
  display: block;
  border-radius: 999px;
}

.summary-line--label {
  width: 42%;
  height: 12px;
}

.summary-line--value {
  width: 66%;
  height: 28px;
  margin-top: 14px;
}

.summary-line--footnote {
  width: 58%;
  height: 12px;
  margin-top: 12px;
}

.shimmer-line {
  position: relative;
  overflow: hidden;
  background: rgba(220, 232, 245, 0.9);
}

.shimmer-line::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.75), transparent);
  animation: summary-shimmer 1.4s ease-in-out infinite;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes summary-shimmer {
  to {
    transform: translateX(100%);
  }
}

@media (max-width: 1199px) {
  .friends-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .friends-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .summary-card,
  .shimmer-line::after {
    animation: none !important;
  }
}
</style>
