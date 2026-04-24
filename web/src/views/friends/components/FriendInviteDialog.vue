<template>
  <div v-if="modelValue" class="invite-dialog-backdrop" @click.self="$emit('update:modelValue', false)">
    <section class="invite-dialog" role="dialog" aria-modal="true" aria-labelledby="friend-invite-title">
      <header class="invite-dialog__head">
        <div class="invite-dialog__title">
          <p class="invite-dialog__kicker">Challenge Setup</p>
          <h3 id="friend-invite-title">发起好友邀战</h3>
          <p>选择出战方式后，立即发送邀请。</p>
        </div>
        <button type="button" class="invite-dialog__close" aria-label="关闭邀战设置" @click="$emit('update:modelValue', false)">
          ×
        </button>
      </header>

      <div v-if="friend" class="invite-dialog__hero">
        <img :src="friend.photo" :alt="friend.name" class="invite-dialog__avatar" />
        <div>
          <strong>{{ friend.name }}</strong>
          <p>{{ friend.signature }}</p>
          <div class="invite-dialog__meta">
            <span>{{ friend.statusLabel }}</span>
            <span>积分 {{ friend.rating }}</span>
          </div>
        </div>
      </div>

      <div class="invite-dialog__body">
        <div class="invite-dialog__section-head">
          <div>
            <p class="invite-dialog__section-kicker">Bot Selection</p>
            <h4>选择出战方式</h4>
          </div>
          <span class="invite-dialog__pill">{{ pendingInvite ? "邀请进行中" : "可发送" }}</span>
        </div>

        <div v-if="loading" class="invite-dialog__grid invite-dialog__grid--loading" aria-hidden="true">
          <span v-for="index in 4" :key="`invite-loading-${index}`" class="invite-loading-card"></span>
        </div>

        <div v-else class="invite-dialog__grid">
          <button
            type="button"
            class="invite-choice"
            :class="{ active: selectedBotId === '-1' }"
            @click="$emit('update:selectedBotId', '-1')"
          >
            <span class="invite-choice__eyebrow">Manual</span>
            <strong>手动操作</strong>
            <p>你亲自操作本局。</p>
          </button>

          <button
            v-for="bot in bots"
            :key="bot.id"
            type="button"
            class="invite-choice"
            :class="{ active: selectedBotId === bot.id }"
            @click="$emit('update:selectedBotId', bot.id)"
          >
            <span class="invite-choice__eyebrow">Bot</span>
            <strong>{{ bot.title }}</strong>
            <p>{{ bot.description || "使用该 Bot 自动出战。" }}</p>
          </button>
        </div>

        <div class="invite-dialog__custom">
          <label class="invite-field">
            <span>房间名</span>
            <input
              type="text"
              class="form-control"
              :value="roomName"
              maxlength="50"
              placeholder="默认好友私人局"
              @input="$emit('update:roomName', $event.target.value)"
            />
          </label>

          <label class="invite-field">
            <span>地图</span>
            <select class="form-select" :value="mapId ?? ''" @change="$emit('update:mapId', $event.target.value)">
              <option value="">随机地图</option>
            </select>
          </label>

          <label class="invite-field">
            <span>回合时限</span>
            <select class="form-select" :value="roundSeconds" @change="$emit('update:roundSeconds', $event.target.value)">
              <option value="10">10 秒</option>
              <option value="15">15 秒</option>
              <option value="30">30 秒</option>
            </select>
          </label>

          <label class="invite-toggle">
            <input
              type="checkbox"
              :checked="allowSpectator"
              @change="$emit('update:allowSpectator', $event.target.checked)"
            />
            <span>允许观战</span>
          </label>
        </div>

        <div class="invite-dialog__summary">
          <div>
            <span>当前配置</span>
            <strong>{{ selectedBotLabel }} · {{ mapLabel }} · {{ roundSeconds }} 秒</strong>
          </div>
          <div>
            <span>发送对象</span>
            <strong>{{ friend?.name || "未选择好友" }}</strong>
          </div>
        </div>
      </div>

      <footer class="invite-dialog__footer">
        <button
          v-if="pendingInvite"
          type="button"
          class="btn btn-outline-secondary invite-dialog__action"
          :disabled="submitting"
          @click="$emit('cancel-invite')"
        >
          {{ submitting ? "处理中..." : "撤回邀战" }}
        </button>
        <button
          type="button"
          class="btn btn-primary invite-dialog__action"
          :disabled="submitting || loading || Boolean(pendingInvite)"
          @click="$emit('confirm')"
        >
          {{ submitting ? "发送中..." : pendingInvite ? "邀请进行中" : "发送邀战" }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  friend: {
    type: Object,
    default: null,
  },
  bots: {
    type: Array,
    default: () => [],
  },
  selectedBotId: {
    type: String,
    default: "-1",
  },
  loading: {
    type: Boolean,
    default: false,
  },
  submitting: {
    type: Boolean,
    default: false,
  },
  pendingInvite: {
    type: Object,
    default: null,
  },
  roomName: {
    type: String,
    default: "",
  },
  mapId: {
    type: [String, Number],
    default: null,
  },
  roundSeconds: {
    type: Number,
    default: 15,
  },
  allowSpectator: {
    type: Boolean,
    default: true,
  },
});

defineEmits([
  "update:modelValue",
  "update:selectedBotId",
  "update:roomName",
  "update:mapId",
  "update:roundSeconds",
  "update:allowSpectator",
  "confirm",
  "cancel-invite",
]);

const selectedBotLabel = computed(() => {
  if (props.selectedBotId === "-1") return "手动操作";
  return props.bots.find((bot) => bot.id === props.selectedBotId)?.title || `Bot #${props.selectedBotId}`;
});

const mapLabel = computed(() => (props.mapId ? `地图 #${props.mapId}` : "随机地图"));
</script>

<style scoped>
.invite-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--kob-z-friends-dialog);
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(6, 20, 32, 0.42);
  backdrop-filter: blur(10px);
}

.invite-dialog {
  width: min(760px, 100%);
  max-height: min(88vh, 860px);
  overflow: auto;
  border-radius: 32px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(242, 249, 255, 0.94)),
    radial-gradient(circle at top right, rgba(255, 208, 93, 0.18), transparent 28%);
  box-shadow: 0 28px 70px rgba(0, 36, 70, 0.22);
}

.invite-dialog__head,
.invite-dialog__footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 24px 26px;
}

.invite-dialog__head {
  border-bottom: 1px solid rgba(90, 180, 255, 0.12);
}

.invite-dialog__kicker,
.invite-dialog__section-kicker,
.invite-choice__eyebrow {
  margin: 0 0 8px;
  color: var(--kob-accent-strong);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.invite-dialog__title h3,
.invite-dialog__section-head h4 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  color: var(--kob-text);
}

.invite-dialog__title p:last-child {
  margin: 10px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
  max-width: 54ch;
}

.invite-dialog__close {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  color: var(--kob-text);
  font-size: 1.2rem;
  line-height: 1;
}

.invite-dialog__hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  margin: 0 26px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(90, 180, 255, 0.16);
  background: rgba(255, 255, 255, 0.78);
}

.invite-dialog__avatar {
  width: 86px;
  height: 86px;
  border-radius: 24px;
  object-fit: cover;
  border: 1px solid rgba(90, 180, 255, 0.18);
}

.invite-dialog__hero strong {
  display: block;
  color: var(--kob-text);
  font-size: 1.08rem;
}

.invite-dialog__hero p {
  margin: 8px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.invite-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.invite-dialog__meta span,
.invite-dialog__pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
}

.invite-dialog__meta span {
  border: 1px solid rgba(90, 180, 255, 0.14);
  background: rgba(245, 249, 255, 0.9);
  color: var(--kob-text);
}

.invite-dialog__body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px 26px 12px;
}

.invite-dialog__section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.invite-dialog__pill {
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
}

.invite-dialog__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.invite-choice,
.invite-loading-card {
  min-height: 172px;
  border-radius: 24px;
}

.invite-choice {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
  padding: 18px;
  border: 1px solid rgba(90, 180, 255, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(245, 250, 255, 0.84)),
    radial-gradient(circle at top right, rgba(61, 174, 255, 0.08), transparent 34%);
  color: inherit;
  text-align: left;
  transition:
    transform var(--motion-fast) var(--ease-out),
    border-color var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.invite-choice strong {
  color: var(--kob-text);
  font-size: 1rem;
}

.invite-choice p {
  margin: 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.invite-choice:hover,
.invite-choice.active {
  transform: translateY(-2px);
  border-color: rgba(61, 174, 255, 0.34);
  box-shadow: 0 14px 28px rgba(0, 50, 100, 0.08);
}

.invite-loading-card {
  position: relative;
  overflow: hidden;
  background: rgba(223, 234, 244, 0.88);
}

.invite-loading-card::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.78), transparent);
  animation: invite-shimmer 1.4s ease-in-out infinite;
}

.invite-dialog__summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.invite-dialog__custom {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr) minmax(0, 0.72fr) auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.14);
  background: rgba(255, 255, 255, 0.68);
}

.invite-field,
.invite-toggle {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invite-field span,
.invite-toggle span {
  color: var(--kob-muted);
  font-size: 0.76rem;
  font-weight: 700;
}

.invite-field .form-control,
.invite-field .form-select {
  min-height: 42px;
  border-radius: 14px;
}

.invite-toggle {
  min-height: 42px;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(90, 180, 255, 0.16);
  background: rgba(245, 250, 255, 0.9);
}

.invite-dialog__summary div {
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.14);
  background: rgba(255, 255, 255, 0.72);
}

.invite-dialog__summary span {
  display: block;
  color: var(--kob-muted);
  font-size: 0.76rem;
}

.invite-dialog__summary strong {
  display: block;
  margin-top: 8px;
  color: var(--kob-text);
}

.invite-dialog__footer {
  justify-content: flex-end;
  padding-top: 18px;
  border-top: 1px solid rgba(90, 180, 255, 0.12);
}

.invite-dialog__action {
  min-height: 46px;
  min-width: 160px;
  border-radius: 999px;
}

@keyframes invite-shimmer {
  to {
    transform: translateX(100%);
  }
}

@media (max-width: 767px) {
  .invite-dialog-backdrop {
    padding: 12px;
  }

  .invite-dialog__head,
  .invite-dialog__footer,
  .invite-dialog__body {
    padding-left: 18px;
    padding-right: 18px;
  }

  .invite-dialog__hero {
    margin: 0 18px;
    grid-template-columns: 1fr;
  }

  .invite-dialog__grid,
  .invite-dialog__custom,
  .invite-dialog__summary {
    grid-template-columns: 1fr;
  }

  .invite-dialog__footer {
    flex-direction: column-reverse;
  }

  .invite-dialog__action {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .invite-choice,
  .invite-loading-card::after {
    transition: none !important;
    animation: none !important;
  }
}
</style>
