<template>
  <section class="detail-panel">
    <div v-if="friend" class="detail-shell">
      <header class="detail-hero">
        <div class="detail-profile">
          <img :src="friend.photo" :alt="friend.name" class="detail-avatar" />
          <div class="detail-copy">
            <p class="detail-kicker">Selected Friend</p>
            <div class="detail-title-row">
              <h3>{{ friend.name }}</h3>
              <button
                type="button"
                class="favorite-chip"
                :class="{ active: friend.favorite }"
                @click="$emit('toggle-favorite', friend.id)"
              >
                {{ friend.favorite ? '已收藏' : '收藏' }}
              </button>
            </div>
            <p class="detail-signature">{{ friend.signature }}</p>
            <p class="detail-note-inline">{{ friend.note }}</p>
            <div class="detail-tags">
              <span class="detail-status" :class="friend.status">{{ friend.statusLabel }}</span>
              <span v-for="tag in friend.tags" :key="tag" class="detail-tag">{{ tag }}</span>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <button type="button" class="btn btn-primary action-btn" @click="$emit('invite', friend.id)">
            发起邀战
          </button>
          <button type="button" class="btn btn-outline-primary action-btn" @click="$emit('message', friend.id)">
            好友私聊
          </button>
          <button type="button" class="btn btn-outline-secondary action-btn" @click="$emit('remove', friend.id)">
            删除好友
          </button>
        </div>
      </header>

      <div class="detail-stats">
        <article class="detail-stat-card">
          <span>当前分数</span>
          <strong>{{ friend.rating }}</strong>
        </article>
        <article class="detail-stat-card">
          <span>共同对局</span>
          <strong>{{ friend.commonMatches }}</strong>
        </article>
        <article class="detail-stat-card">
          <span>胜率记录</span>
          <strong>{{ friend.winRate }}</strong>
        </article>
        <article class="detail-stat-card">
          <span>最近状态</span>
          <strong>{{ friend.lastSeen }}</strong>
        </article>
      </div>

      <div class="detail-body-grid">
        <article class="detail-note-card">
          <p class="card-kicker">备注</p>
          <h4>互动建议</h4>
          <p class="note-copy">{{ friend.note }}</p>
          <dl class="meta-grid">
            <div>
              <dt>地区</dt>
              <dd>{{ friend.region }}</dd>
            </div>
            <div>
              <dt>关系热度</dt>
              <dd>{{ friend.recentlyPlayed ? '近期活跃' : '待重新激活' }}</dd>
            </div>
          </dl>
        </article>

        <article class="detail-activity-card">
          <p class="card-kicker">Timeline</p>
          <h4>最近互动</h4>
          <ul class="activity-list">
            <li v-for="item in friend.activity" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </li>
          </ul>
        </article>
      </div>
    </div>

    <div v-else class="detail-empty">
      <strong>暂无好友详情</strong>
      <p>先去发现用户，添加几位好友。</p>
      <button type="button" class="btn btn-primary" @click="$emit('open-discover')">
        去发现用户
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  friend: {
    type: Object,
    default: null,
  },
});

defineEmits(["invite", "message", "toggle-favorite", "remove", "open-discover"]);
</script>

<style scoped>
.detail-panel {
  min-height: 100%;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 22px;
  border-radius: 26px;
  border: 1px solid rgba(90, 180, 255, 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(245, 251, 255, 0.8)),
    radial-gradient(circle at top right, rgba(255, 208, 93, 0.14), transparent 34%);
}

.detail-profile {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.detail-avatar {
  width: 104px;
  height: 104px;
  border-radius: 30px;
  border: 1px solid rgba(90, 180, 255, 0.28);
  object-fit: cover;
}

.detail-kicker,
.card-kicker {
  margin: 0 0 8px;
  color: var(--kob-accent-strong);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-title-row h3,
.detail-note-card h4,
.detail-activity-card h4 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  color: var(--kob-text);
}

.detail-signature {
  margin: 10px 0 0;
  color: var(--kob-muted);
  line-height: 1.65;
}

.detail-note-inline {
  max-width: 58ch;
  margin: 10px 0 0;
  color: var(--kob-text);
  line-height: 1.65;
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.detail-status,
.detail-tag,
.favorite-chip {
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 0.76rem;
  font-weight: 700;
}

.detail-status {
  background: rgba(61, 174, 255, 0.14);
  color: var(--kob-accent-strong);
}

.detail-status.busy {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.detail-status.offline {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.detail-tag,
.favorite-chip {
  border: 1px solid rgba(90, 180, 255, 0.2);
  background: rgba(255, 255, 255, 0.74);
  color: var(--kob-text);
}

.favorite-chip.active {
  border-color: rgba(255, 183, 50, 0.34);
  background: rgba(255, 244, 214, 0.94);
  color: #8a5a00;
}

.detail-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 340px;
}

.action-btn {
  min-width: 148px;
  min-height: 46px;
  border-radius: 999px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-stat-card,
.detail-note-card,
.detail-activity-card {
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(247, 251, 255, 0.78));
}

.detail-stat-card span {
  display: block;
  color: var(--kob-muted);
  font-size: 0.76rem;
}

.detail-stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--kob-text);
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.18rem;
  line-height: 1.25;
}

.detail-body-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 14px;
}

.note-copy {
  margin: 0;
  color: var(--kob-text);
  line-height: 1.7;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 0;
}

.meta-grid dt {
  color: var(--kob-muted);
  font-size: 0.74rem;
}

.meta-grid dd {
  margin: 6px 0 0;
  color: var(--kob-text);
  font-weight: 600;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.activity-list li {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 0 0 12px;
  border-bottom: 1px solid rgba(90, 180, 255, 0.14);
}

.activity-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.activity-list span {
  color: var(--kob-muted);
}

.activity-list strong {
  color: var(--kob-text);
  text-align: right;
}

.detail-empty {
  padding: 28px;
  border-radius: 22px;
  border: 1px dashed rgba(90, 180, 255, 0.26);
  background: rgba(246, 251, 255, 0.78);
}

.detail-empty strong {
  display: block;
  color: var(--kob-text);
}

.detail-empty p {
  margin: 10px 0 18px;
  color: var(--kob-muted);
  line-height: 1.65;
}

@media (max-width: 1199px) {
  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-body-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .detail-hero {
    flex-direction: column;
  }

  .detail-profile,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .detail-stats {
    grid-template-columns: 1fr;
  }

  .detail-actions {
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }

  .activity-list li {
    display: block;
  }

  .activity-list strong {
    display: block;
    margin-top: 6px;
    text-align: left;
  }
}
</style>
