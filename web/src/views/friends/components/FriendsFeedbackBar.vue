<template>
  <transition name="feedback-pop">
    <section v-if="feedback?.visible" class="feedback-bar" :class="feedback.type" aria-live="polite">
      <div>
        <strong>{{ feedback.title }}</strong>
        <p>{{ feedback.message }}</p>
      </div>
      <button type="button" class="feedback-close" aria-label="关闭反馈" @click="$emit('close')">
        关闭
      </button>
    </section>
  </transition>
</template>

<script setup>
defineProps({
  feedback: {
    type: Object,
    default: null,
  },
});

defineEmits(["close"]);
</script>

<style scoped>
.feedback-bar {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(244, 250, 255, 0.82));
  box-shadow: 0 16px 34px rgba(0, 50, 100, 0.08);
}

.feedback-bar.success {
  border-color: rgba(34, 197, 94, 0.28);
  background: rgba(240, 253, 244, 0.92);
}

.feedback-bar.warn {
  border-color: rgba(245, 158, 11, 0.28);
  background: rgba(255, 251, 235, 0.92);
}

.feedback-bar strong {
  display: block;
  color: var(--kob-text);
}

.feedback-bar p {
  margin: 4px 0 0;
  color: var(--kob-muted);
}

.feedback-close {
  flex: none;
  border: 0;
  border-radius: 999px;
  min-height: 42px;
  padding: 8px 14px;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
  font-weight: 700;
}

.feedback-pop-enter-active,
.feedback-pop-leave-active {
  transition:
    opacity var(--motion-medium) var(--ease-out),
    transform var(--motion-medium) var(--ease-out);
}

.feedback-pop-enter-from,
.feedback-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
