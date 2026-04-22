<template>
  <article class="friend-list-item" :class="{ active }">
    <button
      type="button"
      class="friend-main"
      :aria-pressed="active"
      @click="$emit('select', friend.id)"
    >
      <div class="friend-avatar-wrap">
        <img :src="friend.photo" :alt="friend.name" class="friend-avatar" />
        <span class="friend-status" :class="friend.status"></span>
      </div>
      <div class="friend-copy">
        <div class="friend-name-row">
          <strong>{{ friend.name }}</strong>
          <span class="friend-status-label">{{ friend.statusLabel }}</span>
        </div>
        <p>{{ friend.signature }}</p>
        <div class="friend-meta-row">
          <small>{{ friend.lastSeen }}</small>
          <span class="friend-score">积分 {{ friend.rating }}</span>
        </div>
        <div class="friend-tag-row">
          <span class="friend-chip">{{ friend.commonMatches }} 场对局</span>
          <span v-if="friend.newToday" class="friend-chip friend-chip--fresh">今日新增</span>
        </div>
      </div>
    </button>

    <button
      type="button"
      class="favorite-toggle"
      :class="{ active: friend.favorite }"
      :aria-pressed="friend.favorite"
      :aria-label="friend.favorite ? `取消收藏 ${friend.name}` : `收藏 ${friend.name}`"
      @click="$emit('toggle-favorite', friend.id)"
    >
      {{ friend.favorite ? '已收藏' : '收藏' }}
    </button>
  </article>
</template>

<script setup>
defineProps({
  friend: {
    type: Object,
    required: true,
  },
  active: {
    type: Boolean,
    default: false,
  },
});

defineEmits(["select", "toggle-favorite"]);
</script>

<style scoped>
.friend-list-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 18px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.58);
  transition:
    transform var(--motion-fast) var(--ease-out),
    border-color var(--motion-fast) var(--ease-out),
    background-color var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out);
}

.friend-list-item:hover,
.friend-list-item.active {
  transform: translateY(-1px) scale(1.005);
  border-color: rgba(90, 180, 255, 0.28);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 24px rgba(0, 50, 100, 0.07);
}

.friend-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
}

.friend-avatar-wrap {
  position: relative;
  width: 56px;
  height: 56px;
}

.friend-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  border: 1px solid rgba(90, 180, 255, 0.25);
  object-fit: cover;
}

.friend-status {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 13px;
  height: 13px;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.friend-status.online {
  background: #22c55e;
}

.friend-status.busy {
  background: #f59e0b;
}

.friend-status.offline {
  background: #94a3b8;
}

.friend-copy {
  min-width: 0;
}

.friend-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.friend-name-row strong {
  min-width: 0;
  font-size: 0.98rem;
  color: var(--kob-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-status-label {
  flex: none;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
  font-size: 0.72rem;
  font-weight: 700;
}

.friend-copy p {
  margin: 6px 0 4px;
  color: var(--kob-text);
  font-size: 0.84rem;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-copy small {
  color: var(--kob-muted);
  font-size: 0.75rem;
}

.friend-meta-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.friend-score,
.friend-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
}

.friend-score {
  color: var(--kob-text);
}

.friend-tag-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.friend-chip {
  padding: 4px 8px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background: rgba(247, 251, 255, 0.82);
  color: var(--kob-muted);
}

.friend-chip--fresh {
  border-color: rgba(245, 158, 11, 0.2);
  background: rgba(255, 248, 235, 0.92);
  color: #b45309;
}

.favorite-toggle {
  border: 1px solid rgba(90, 180, 255, 0.26);
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--kob-muted);
  font-size: 0.76rem;
  font-weight: 700;
  transition:
    border-color var(--motion-fast) var(--ease-out),
    color var(--motion-fast) var(--ease-out),
    background-color var(--motion-fast) var(--ease-out);
}

.favorite-toggle:hover,
.favorite-toggle.active {
  border-color: rgba(255, 183, 50, 0.42);
  background: rgba(255, 239, 204, 0.9);
  color: #8a5a00;
}

@media (max-width: 767px) {
  .friend-list-item {
    grid-template-columns: 1fr;
  }

  .friend-meta-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .favorite-toggle {
    width: 100%;
  }
}
</style>
