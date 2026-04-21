<template>
  <teleport to="body">
    <div v-if="modelValue" class="app-dialog-backdrop" @click.self="handleCancel">
      <div
        ref="dialogRef"
        class="app-dialog"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="messageId"
      >
        <div class="app-dialog-header">
          <h5 :id="titleId" class="app-dialog-title">{{ title }}</h5>
          <button
            ref="closeButtonRef"
            type="button"
            class="app-dialog-close"
            aria-label="关闭弹窗"
            @click="handleCancel"
          >
            ×
          </button>
        </div>
        <div :id="messageId" class="app-dialog-body">{{ message }}</div>
        <div class="app-dialog-footer">
          <button
            v-if="showCancel"
            ref="cancelButtonRef"
            type="button"
            class="btn-cancel"
            @click="handleCancel"
          >
            {{ cancelText }}
          </button>
          <button ref="confirmButtonRef" type="button" class="btn-confirm" @click="handleConfirm">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: "提示" },
  message: { type: String, default: "" },
  confirmText: { type: String, default: "确定" },
  cancelText: { type: String, default: "取消" },
  showCancel: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue", "confirm", "cancel"]);

const dialogRef = ref(null);
const closeButtonRef = ref(null);
const cancelButtonRef = ref(null);
const confirmButtonRef = ref(null);
const previousActiveElement = ref(null);

const uid = Math.random().toString(36).slice(2, 10);
const titleId = computed(() => `app-dialog-title-${uid}`);
const messageId = computed(() => `app-dialog-message-${uid}`);

/**
 * Handles getFocusableElements.
 * ??getFocusableElements?
 */
const getFocusableElements = () => {
  const root = dialogRef.value;
  if (!root) return [];
  return Array.from(
    root.querySelectorAll(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )
  );
};

const focusPreferredButton = async () => {
  await nextTick();
  const target = confirmButtonRef.value || cancelButtonRef.value || closeButtonRef.value;
  if (target && typeof target.focus === "function") {
    target.focus();
  }
};

/**
 * Handles handleKeydown.
 * ??handleKeydown?
 */
const handleKeydown = (event) => {
  if (!props.modelValue) return;

  if (event.key === "Escape") {
    event.preventDefault();
    handleCancel();
    return;
  }

  if (event.key !== "Tab") return;
  const focusables = getFocusableElements();
  if (focusables.length === 0) return;

  const first = focusables[0];
  const last = focusables[focusables.length - 1];
  const active = document.activeElement;

  if (!event.shiftKey && active === last) {
    event.preventDefault();
    first.focus();
  } else if (event.shiftKey && active === first) {
    event.preventDefault();
    last.focus();
  }
};

/**
 * Handles close.
 * ??close?
 */
const close = () => {
  if (!props.modelValue) return;
  emit("update:modelValue", false);
};

/**
 * Handles handleConfirm.
 * ??handleConfirm?
 */
const handleConfirm = () => {
  emit("confirm");
  close();
};

/**
 * Handles handleCancel.
 * ??handleCancel?
 */
const handleCancel = () => {
  emit("cancel");
  close();
};

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      previousActiveElement.value = document.activeElement;
      document.addEventListener("keydown", handleKeydown);
      await focusPreferredButton();
      return;
    }

    document.removeEventListener("keydown", handleKeydown);
    if (
      previousActiveElement.value &&
      typeof previousActiveElement.value.focus === "function"
    ) {
      previousActiveElement.value.focus();
    }
    previousActiveElement.value = null;
  }
);

onBeforeUnmount(() => {
  document.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
.app-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--kob-z-app-dialog);
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-text);
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
}

.app-dialog-body {
  padding: 16px;
  line-height: 1.6;
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
  min-height: 40px;
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
