<template>
  <aside class="friends-sidebar">
    <div class="sidebar-head">
      <div>
        <p class="panel-kicker">Roster</p>
        <h3>好友列表</h3>
        <p class="sidebar-copy">在线、常用、最近互动，一眼可见。</p>
      </div>
      <button type="button" class="ghost-pill" @click="$emit('open-discover')">
        发现用户
      </button>
    </div>

    <label class="search-shell" for="friend-roster-search">
      <span class="search-label">快速筛选</span>
      <input
        id="friend-roster-search"
        type="text"
        class="form-control sidebar-search"
        :value="searchValue"
        placeholder="搜索昵称或状态"
        @input="$emit('update:search', $event.target.value)"
      />
    </label>

    <div class="sidebar-segments" role="tablist" aria-label="好友筛选分组">
      <button
        v-for="segment in segments"
        :key="segment.key"
        type="button"
        class="segment-chip"
        :class="{ active: activeSegment === segment.key }"
        :aria-selected="activeSegment === segment.key"
        @click="$emit('select-segment', segment.key)"
      >
        <span>{{ segment.label }}</span>
        <small>{{ segment.count }}</small>
      </button>
    </div>

    <div class="sidebar-meta" aria-live="polite">
      <span>当前分组：{{ activeSegmentLabel }}</span>
      <strong>{{ friends.length }} 位结果</strong>
    </div>

    <div v-if="friends.length" class="friend-list">
      <FriendListItem
        v-for="friend in friends"
        :key="friend.id"
        :friend="friend"
        :active="selectedFriendId === friend.id"
        @select="$emit('select-friend', $event)"
        @toggle-favorite="$emit('toggle-favorite', $event)"
      />
    </div>

    <div v-else class="sidebar-empty">
      <strong>没有匹配结果</strong>
      <p>试试切换分组，或去发现用户添加好友。</p>
      <button type="button" class="btn btn-outline-primary" @click="$emit('open-discover')">
        去发现用户
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from "vue";
import FriendListItem from "./FriendListItem.vue";

const props = defineProps({
  segments: {
    type: Array,
    default: () => [],
  },
  activeSegment: {
    type: String,
    default: "all",
  },
  searchValue: {
    type: String,
    default: "",
  },
  friends: {
    type: Array,
    default: () => [],
  },
  selectedFriendId: {
    type: Number,
    default: null,
  },
});

defineEmits([
  "open-discover",
  "update:search",
  "select-segment",
  "select-friend",
  "toggle-favorite",
]);

const activeSegmentLabel = computed(
  () => props.segments.find((segment) => segment.key === props.activeSegment)?.label || "全部好友"
);
</script>

<style scoped>
.friends-sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
  padding: 20px;
  border-radius: 26px;
  border: 1px solid rgba(90, 180, 255, 0.24);
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 18px 42px rgba(0, 50, 100, 0.08);
  backdrop-filter: blur(10px);
}

.sidebar-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.panel-kicker {
  margin: 0 0 6px;
  color: var(--kob-accent-strong);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sidebar-head h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.22rem;
}

.sidebar-copy {
  max-width: 26ch;
  margin: 10px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.ghost-pill {
  flex: none;
  border: 1px solid rgba(90, 180, 255, 0.24);
  border-radius: 999px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--kob-accent-strong);
  font-weight: 700;
}

.search-shell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-label {
  color: var(--kob-muted);
  font-size: 0.8rem;
  font-weight: 700;
}

.sidebar-search {
  min-height: 46px;
  border-radius: 16px;
}

.sidebar-segments {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.segment-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(90, 180, 255, 0.2);
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--kob-muted);
  font-size: 0.76rem;
  font-weight: 700;
  transition:
    color var(--motion-fast) var(--ease-out),
    border-color var(--motion-fast) var(--ease-out),
    background-color var(--motion-fast) var(--ease-out);
}

.segment-chip small {
  color: inherit;
  font-size: 0.72rem;
}

.segment-chip.active,
.segment-chip:hover {
  border-color: rgba(90, 180, 255, 0.34);
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
}

.sidebar-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(90, 180, 255, 0.14);
  background: rgba(248, 252, 255, 0.82);
}

.sidebar-meta span {
  color: var(--kob-muted);
  font-size: 0.78rem;
}

.sidebar-meta strong {
  color: var(--kob-text);
  font-family: "Space Grotesk", sans-serif;
  font-size: 0.94rem;
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-empty {
  padding: 18px;
  border-radius: 18px;
  border: 1px dashed rgba(90, 180, 255, 0.28);
  background: rgba(244, 250, 255, 0.72);
}

.sidebar-empty strong {
  display: block;
  color: var(--kob-text);
}

.sidebar-empty p {
  margin: 8px 0 14px;
  color: var(--kob-muted);
  line-height: 1.6;
}

@media (max-width: 991px) {
  .friends-sidebar {
    border-radius: 22px;
  }

  .sidebar-head {
    flex-direction: column;
  }
}
</style>
