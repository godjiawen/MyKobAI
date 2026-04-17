<template>
  <section class="request-panel">
    <div class="request-grid">
      <article class="request-column">
        <header class="column-head">
          <div>
            <p class="panel-kicker">Inbox</p>
            <h3>收到的申请</h3>
            <p class="column-copy">这里处理别人主动发来的关系请求，优先消化待决事项。</p>
          </div>
          <span class="count-badge">{{ incomingRequests.length }}</span>
        </header>

        <div v-if="incomingRequests.length" class="request-list">
          <article v-for="request in incomingRequests" :key="request.id" class="request-card">
            <div class="request-top">
              <img :src="request.photo" :alt="request.name" class="request-avatar" />
              <div class="request-copy">
                <div class="request-title-row">
                  <strong>{{ request.name }}</strong>
                  <span>{{ request.timeLabel }}</span>
                </div>
                <p>{{ request.message }}</p>
                <div class="request-tags">
                  <span class="request-status">{{ request.statusLabel }}</span>
                  <span v-for="tag in request.tags" :key="tag" class="request-tag">{{ tag }}</span>
                </div>
              </div>
            </div>
            <div class="request-meta">
              <span>分数 {{ request.rating }}</span>
              <span>{{ request.region }}</span>
            </div>
            <div class="request-actions">
              <button type="button" class="btn btn-primary" @click="$emit('accept', request.id)">通过申请</button>
              <button type="button" class="btn btn-outline-secondary" @click="$emit('ignore', request.id)">忽略</button>
            </div>
          </article>
        </div>

        <div v-else class="request-empty">
          <strong>没有待处理的好友申请</strong>
          <p>当其他用户向你发出好友请求时，这里会优先承载审批动作。</p>
        </div>
      </article>

      <article class="request-column">
        <header class="column-head">
          <div>
            <p class="panel-kicker">Outbox</p>
            <h3>你发出的申请</h3>
            <p class="column-copy">外发申请集中放在这里，避免忘记已经建立过的接触动作。</p>
          </div>
          <span class="count-badge">{{ outgoingRequests.length }}</span>
        </header>

        <div v-if="outgoingRequests.length" class="request-list">
          <article v-for="request in outgoingRequests" :key="request.id" class="request-card">
            <div class="request-top">
              <img :src="request.photo" :alt="request.name" class="request-avatar" />
              <div class="request-copy">
                <div class="request-title-row">
                  <strong>{{ request.name }}</strong>
                  <span>{{ request.timeLabel }}</span>
                </div>
                <p>{{ request.message }}</p>
                <div class="request-tags">
                  <span class="request-status pending">等待回应</span>
                  <span v-for="tag in request.tags" :key="tag" class="request-tag">{{ tag }}</span>
                </div>
              </div>
            </div>
            <div class="request-meta">
              <span>分数 {{ request.rating }}</span>
              <span>{{ request.region }}</span>
            </div>
            <div class="request-actions single">
              <button type="button" class="btn btn-outline-secondary" @click="$emit('cancel', request.id)">撤回申请</button>
            </div>
          </article>
        </div>

        <div v-else class="request-empty">
          <strong>还没有发出的申请</strong>
          <p>发现到合适的用户后，可以在发现用户面板中直接发起好友申请。</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  incomingRequests: {
    type: Array,
    default: () => [],
  },
  outgoingRequests: {
    type: Array,
    default: () => [],
  },
});

defineEmits(["accept", "ignore", "cancel"]);
</script>

<style scoped>
.request-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.request-column {
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(90, 180, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(244, 250, 255, 0.8));
}

.panel-kicker {
  margin: 0 0 6px;
  color: var(--kob-accent-strong);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.column-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.column-head h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-size: 1.18rem;
}

.column-copy {
  max-width: 32ch;
  margin: 10px 0 0;
  color: var(--kob-muted);
  line-height: 1.6;
}

.count-badge {
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
  font-weight: 700;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(90, 180, 255, 0.16);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 24px rgba(0, 50, 100, 0.05);
}

.request-top {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
}

.request-avatar {
  width: 58px;
  height: 58px;
  border-radius: 18px;
}

.request-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.request-title-row strong {
  color: var(--kob-text);
}

.request-title-row span,
.request-meta span {
  color: var(--kob-muted);
  font-size: 0.76rem;
}

.request-copy p {
  margin: 8px 0;
  color: var(--kob-text);
  line-height: 1.6;
}

.request-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.request-status,
.request-tag {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.74rem;
  font-weight: 700;
}

.request-status {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.request-status.pending {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.request-tag {
  border: 1px solid rgba(90, 180, 255, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: var(--kob-text);
}

.request-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.request-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.request-actions.single .btn {
  width: 100%;
}

.request-actions .btn {
  border-radius: 999px;
  min-height: 44px;
}

.request-empty {
  padding: 22px;
  border-radius: 18px;
  border: 1px dashed rgba(90, 180, 255, 0.24);
  background: rgba(246, 251, 255, 0.76);
}

.request-empty strong {
  display: block;
  color: var(--kob-text);
}

.request-empty p {
  margin: 8px 0 0;
  color: var(--kob-muted);
  line-height: 1.6;
}

@media (max-width: 991px) {
  .request-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .request-top,
  .request-title-row {
    grid-template-columns: 1fr;
  }

  .request-title-row {
    display: block;
  }

  .request-title-row span {
    display: block;
    margin-top: 6px;
  }

  .request-actions {
    flex-direction: column;
  }

  .request-actions .btn {
    width: 100%;
  }
}
</style>
