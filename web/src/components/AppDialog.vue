<template>
  <div v-if="modelValue" class="app-dialog-backdrop" @click.self="handleCancel">
    <div class="app-dialog" role="dialog" aria-modal="true">
      <div class="app-dialog-header">
        <h5 class="app-dialog-title">{{ title }}</h5>
        <button type="button" class="app-dialog-close" @click="handleCancel">x</button>
      </div>
      <div class="app-dialog-body">{{ message }}</div>
      <div class="app-dialog-footer">
        <button v-if="showCancel" type="button" class="btn-cancel" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button type="button" class="btn-confirm" @click="handleConfirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: "Notice" },
  message: { type: String, default: "" },
  confirmText: { type: String, default: "OK" },
  cancelText: { type: String, default: "Cancel" },
  showCancel: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue", "confirm", "cancel"]);

const close = () => {
  if (!props.modelValue) return;
  emit("update:modelValue", false);
};

const handleConfirm = () => {
  emit("confirm");
  close();
};

const handleCancel = () => {
  emit("cancel");
  close();
};
</script>

<style scoped>
.app-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 3200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 20, 40, 0.45);
  backdrop-filter: blur(4px);
}

.app-dialog {
  width: min(460px, 92vw);
  border-radius: 18px;
  border: 1px solid var(--kob-panel-border);
  background: var(--kob-panel);
  color: var(--kob-text);
  box-shadow: 0 20px 48px rgba(0, 30, 70, 0.22);
  overflow: hidden;
}

.app-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(90, 180, 255, 0.28);
}

.app-dialog-title {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 1rem;
  font-weight: 700;
}

.app-dialog-close {
  border: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-text);
  font-weight: 700;
  cursor: pointer;
}

.app-dialog-body {
  padding: 16px;
  line-height: 1.5;
  white-space: pre-line;
  color: var(--kob-text);
}

.app-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px 16px;
}

.btn-cancel,
.btn-confirm {
  border: 0;
  border-radius: 999px;
  padding: 8px 16px;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel {
  background: rgba(90, 180, 255, 0.16);
  color: var(--kob-text);
}

.btn-confirm {
  color: #fff;
  background: linear-gradient(135deg, var(--kob-accent-strong), var(--kob-accent));
}
</style>
