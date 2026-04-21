<template>
  <section class="discover-panel" :class="{ 'is-loading': loading }">
    <div v-if="loading" class="discover-grid discover-grid--loading" aria-hidden="true">
      <article v-for="index in skeletonCount" :key="`discover-skeleton-${index}`" class="discover-card skeleton-card">
        <div class="skeleton-card-head">
          <span class="skeleton-rank"></span>
          <span class="skeleton-rank skeleton-rank--small"></span>
        </div>
        <div class="skeleton-head">
          <span class="skeleton-avatar"></span>
          <div class="skeleton-lines">
            <span class="skeleton-line skeleton-line--title"></span>
            <span class="skeleton-line"></span>
          </div>
        </div>
        <span class="skeleton-line skeleton-line--meta"></span>
        <div class="skeleton-tags">
          <span class="skeleton-chip"></span>
          <span class="skeleton-chip"></span>
        </div>
        <span class="skeleton-copy"></span>
        <span class="skeleton-button"></span>
      </article>
    </div>

    <transition-group v-else-if="users.length" name="discover-card" tag="div" class="discover-grid">
      <article
        v-for="(user, index) in users"
        :key="user.id"
        class="discover-card"
        :style="{ '--card-delay': `${index * 40}ms` }"
      >
        <div class="discover-card-head">
          <span class="discover-rank">TOP {{ index + 1 }}</span>
          <span class="discover-score">积分 {{ user.rating }}</span>
        </div>

        <div class="discover-head">
          <img :src="user.photo" :alt="user.name" class="discover-avatar" />
          <div class="discover-identity">
            <strong>{{ user.name }}</strong>
            <p>{{ user.signature }}</p>
          </div>
        </div>

        <div class="discover-meta">
          <span>状态：{{ primaryTag(user) }}</span>
          <span>{{ user.region }}</span>
        </div>

        <div class="discover-tags">
          <span class="relation-chip" :class="user.relation">{{ relationLabel(user.relation) }}</span>
          <span v-for="tag in user.tags" :key="tag" class="discover-tag">{{ tag }}</span>
        </div>

        <p class="discover-note">{{ decisionHint(user.relation) }}</p>

        <button
          type="button"
          class="btn action-btn"
          :class="actionClass(user.relation)"
          :disabled="user.relation === 'pending'"
          @click="handleAction(user)"
        >
          {{ actionLabel(user.relation) }}
        </button>
      </article>
    </transition-group>

    <div v-else class="discover-empty">
      <strong>没有找到匹配用户</strong>
      <p>换个关键词再试试。</p>
      <button type="button" class="btn btn-outline-primary empty-action" @click="$emit('clear-search')">
        清空搜索
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";

const emit = defineEmits(["send-request", "open-requests", "open-friend", "clear-search"]);

const props = defineProps({
  users: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  displayLimit: {
    type: Number,
    default: 9,
  },
  totalCount: {
    type: Number,
    default: 0,
  },
});

const skeletonCount = computed(() => Math.min(props.displayLimit, 6));

/**
 * Handles relationLabel.
 * ??relationLabel?
 */
const relationLabel = (relation) => {
  const labels = {
    none: "可添加",
    pending: "已发送",
    incoming: "待处理",
    friend: "已是好友",
  };
  return labels[relation] || "可添加";
};

/**
 * Handles actionLabel.
 * ??actionLabel?
 */
const actionLabel = (relation) => {
  const labels = {
    none: "添加好友",
    pending: "等待回应",
    incoming: "去处理申请",
    friend: "查看好友",
  };
  return labels[relation] || "添加好友";
};

/**
 * Handles actionClass.
 * ??actionClass?
 */
const actionClass = (relation) => {
  if (relation === "incoming") return "btn-outline-warning";
  if (relation === "pending") return "btn-outline-secondary";
  if (relation === "friend") return "btn-outline-primary";
  return "btn-primary";
};

const primaryTag = (user) => user.tags?.[0] || "离线";

/**
 * Handles decisionHint.
 * ??decisionHint?
 */
const decisionHint = (relation) => {
  const hints = {
    none: "可以直接发送好友申请。",
    pending: "申请已发出，等对方回应。",
    incoming: "对方已申请你，去申请页处理。",
    friend: "已经是好友，可直接互动。",
  };
  return hints[relation] || hints.none;
};

/**
 * Handles handleAction.
 * ??handleAction?
 */
const handleAction = (user) => {
  if (user.relation === "incoming") {
    emit("open-requests");
    return;
  }
  if (user.relation === "friend") {
    emit("open-friend", user.id);
    return;
  }
  if (user.relation === "pending") return;
  emit("send-request", user.id);
};
</script>

<style scoped>
.discover-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.discover-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.discover-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(244, 250, 255, 0.84)),
    radial-gradient(circle at top right, rgba(61, 174, 255, 0.1), transparent 38%);
  box-shadow: 0 12px 28px rgba(0, 50, 100, 0.06);
}

.discover-card:nth-child(3n + 2) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(247, 253, 246, 0.86)),
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.1), transparent 38%);
}

.discover-card:nth-child(3n) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 249, 239, 0.88)),
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.12), transparent 38%);
}

.discover-card-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.discover-rank,
.discover-score {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 700;
}

.discover-rank {
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
}

.discover-score {
  background: rgba(255, 255, 255, 0.76);
  color: var(--kob-text);
}

.discover-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.discover-avatar {
  width: 68px;
  height: 68px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  object-fit: cover;
}

.discover-identity strong {
  display: block;
  color: var(--kob-text);
  font-size: 1.02rem;
}

.discover-identity p {
  margin: 6px 0 0;
  color: var(--kob-muted);
  line-height: 1.6;
}

.discover-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--kob-muted);
  font-size: 0.78rem;
}

.discover-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.relation-chip,
.discover-tag {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 700;
}

.relation-chip.none {
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
}

.relation-chip.pending {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.relation-chip.incoming {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.relation-chip.friend {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.discover-tag {
  border: 1px solid rgba(90, 180, 255, 0.18);
  background: rgba(255, 255, 255, 0.74);
  color: var(--kob-text);
}

.discover-note {
  margin: 0;
  color: var(--kob-text);
  line-height: 1.68;
}

.action-btn,
.empty-action {
  min-height: 46px;
  margin-top: auto;
  border-radius: 999px;
  transition:
    transform var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.action-btn:hover:not(:disabled),
.empty-action:hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 10px 20px rgba(0, 50, 100, 0.08);
}

.discover-empty {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  padding: 24px;
  border-radius: 22px;
  border: 1px dashed rgba(90, 180, 255, 0.24);
  background: rgba(246, 251, 255, 0.76);
}

.discover-empty strong {
  display: block;
  color: var(--kob-text);
}

.discover-empty p {
  margin: 0;
  color: var(--kob-muted);
  line-height: 1.7;
}

.skeleton-card {
  pointer-events: none;
}

.skeleton-card-head,
.skeleton-head,
.skeleton-tags {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.skeleton-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
}

.skeleton-rank,
.skeleton-avatar,
.skeleton-line,
.skeleton-chip,
.skeleton-copy,
.skeleton-button {
  position: relative;
  overflow: hidden;
  background: rgba(220, 232, 245, 0.9);
}

.skeleton-rank::after,
.skeleton-avatar::after,
.skeleton-line::after,
.skeleton-chip::after,
.skeleton-copy::after,
.skeleton-button::after {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.75), transparent);
  animation: shimmer 1.4s ease-in-out infinite;
}

.skeleton-rank {
  width: 74px;
  height: 28px;
  border-radius: 999px;
}

.skeleton-rank--small {
  width: 84px;
}

.skeleton-avatar {
  width: 68px;
  height: 68px;
  border-radius: 22px;
}

.skeleton-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  display: block;
  width: 100%;
  height: 12px;
  border-radius: 999px;
}

.skeleton-line--title {
  width: 58%;
  height: 14px;
}

.skeleton-line--meta {
  width: 42%;
}

.skeleton-chip {
  width: 74px;
  height: 28px;
  border-radius: 999px;
}

.skeleton-copy {
  width: 100%;
  height: 42px;
  border-radius: 18px;
}

.skeleton-button {
  width: 100%;
  height: 46px;
  margin-top: auto;
  border-radius: 999px;
}

.discover-card-enter-active {
  animation: discover-rise 360ms var(--ease-out) both;
  animation-delay: var(--card-delay);
}

.discover-card-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.discover-card-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

@keyframes discover-rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  to {
    transform: translateX(100%);
  }
}

@media (max-width: 1199px) {
  .discover-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .discover-grid {
    grid-template-columns: 1fr;
  }

  .discover-head {
    grid-template-columns: 1fr;
  }

  .discover-card-head {
    flex-wrap: wrap;
  }
}

@media (prefers-reduced-motion: reduce) {
  .discover-card,
  .action-btn,
  .empty-action,
  .discover-card-enter-active,
  .discover-card-leave-active {
    transition: none !important;
    animation: none !important;
  }

  .skeleton-rank::after,
  .skeleton-avatar::after,
  .skeleton-line::after,
  .skeleton-chip::after,
  .skeleton-copy::after,
  .skeleton-button::after {
    animation: none !important;
  }
}
</style>
