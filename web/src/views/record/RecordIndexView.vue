<template>
  <ContentField>
    <section class="record-view">
      <header class="view-head">
        <div>
          <p class="kob-kicker">Replay Center</p>
          <h2 class="kob-headline">对局记录</h2>
          <p class="kob-subtitle">复盘每一场对局，查看双方结果与时间轴。</p>
        </div>
        <span class="stats-badge">总记录 {{ totalRecords }}</span>
      </header>

      <div class="kob-table-wrap">
        <table class="table table-hover align-middle list-table" :aria-busy="loading">
          <thead>
            <tr>
              <th>玩家 A</th>
              <th>玩家 B</th>
              <th>结果</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="row in skeletonRows" :key="`record-skeleton-${row}`">
              <td colspan="5">
                <div class="skeleton-line kob-shimmer"></div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="record in records" :key="record.id">
              <td>
                <img :src="record.a_photo || fallbackPhoto" alt="" class="kob-avatar-36" />
                <span class="username">{{ record.a_username || "-" }}</span>
              </td>
              <td>
                <img :src="record.b_photo || fallbackPhoto" alt="" class="kob-avatar-36" />
                <span class="username">{{ record.b_username || "-" }}</span>
              </td>
              <td>{{ record.result || "-" }}</td>
              <td>{{ record.record?.createtime || "-" }}</td>
              <td>
                <button @click="openRecordContent(record.record?.id)" type="button" class="btn btn-primary kob-pill-btn replay-btn">
                  查看回放
                </button>
              </td>
            </tr>
            <tr v-if="!errorMessage && records.length === 0">
              <td colspan="5" class="empty-row">暂无回放记录</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="loading" class="kob-status-inline">回放列表加载中...</p>
      <p v-if="errorMessage" class="kob-status-inline kob-status-error">{{ errorMessage }}</p>

      <nav aria-label="回放分页" class="pager-wrap">
        <ul class="pagination">
          <li :class="['page-item', { disabled: loading || !canPrev }]">
            <button class="page-link page-btn" type="button" :disabled="loading || !canPrev" @click="clickPage(-2)">
              上一页
            </button>
          </li>
          <li :class="`page-item ${page.is_active}`" v-for="page in pages" :key="page.number">
            <button class="page-link page-btn" type="button" :disabled="loading" @click="clickPage(page.number)">
              {{ page.number }}
            </button>
          </li>
          <li :class="['page-item', { disabled: loading || !canNext }]">
            <button class="page-link page-btn" type="button" :disabled="loading || !canNext" @click="clickPage(-1)">
              下一页
            </button>
          </li>
        </ul>
      </nav>
    </section>
  </ContentField>
</template>

<script setup>
defineOptions({
  name: "RecordIndexView",
});

import { computed, onActivated, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import defaultAvatar from "@/assets/images/default-avatar.svg";

const userStore = useUserStore();
const pkStore = usePkStore();
const recordStore = useRecordStore();
const router = useRouter();

const records = ref([]);
const pages = ref([]);
const loading = ref(true);
const errorMessage = ref("");
const currentPage = ref(1);
const totalRecords = ref(0);
const hasMounted = ref(false);
const skeletonRows = [1, 2, 3, 4, 5];
const fallbackPhoto = defaultAvatar;

const maxPages = computed(() => Math.ceil(totalRecords.value / 10));
const canPrev = computed(() => currentPage.value > 1);
const canNext = computed(() => currentPage.value < maxPages.value);

/**
 * Handles updatePages.
 * ??updatePages?
 */
const updatePages = () => {
  const nextPages = [];

  for (let i = currentPage.value - 2; i <= currentPage.value + 2; i += 1) {
    if (i >= 1 && i <= maxPages.value) {
      nextPages.push({
        number: i,
        is_active: i === currentPage.value ? "active" : "",
      });
    }
  }

  pages.value = nextPages;
};

const pullPage = async (page, { force = false } = {}) => {
  if (loading.value && !force) return;

  currentPage.value = page;
  loading.value = true;
  errorMessage.value = "";
  try {
    const resp = await apiRequest(API_PATHS.records, {
      data: { page },
      token: userStore.token,
    });
    const list = Array.isArray(resp.records) ? resp.records : [];
    records.value = list.map((item) => ({
      ...item,
      record: item.record || {},
    }));
    totalRecords.value = resp.records_count || 0;
    updatePages();
  } catch (error) {
    console.error(error);
    records.value = [];
    totalRecords.value = 0;
    pages.value = [];
    errorMessage.value = "回放列表加载失败，请确认后端接口是否可用。";
  } finally {
    loading.value = false;
  }
};

/**
 * Handles clickPage.
 * ??clickPage?
 */
const clickPage = (page) => {
  if (loading.value) return;

  let target = page;
  if (target === -2) target = currentPage.value - 1;
  if (target === -1) target = currentPage.value + 1;

  if (target >= 1 && target <= maxPages.value) {
    pullPage(target);
  }
};

/**
 * Handles stringTo2D.
 * ??stringTo2D?
 */
const stringTo2D = (map) => {
  const graph = [];
  for (let i = 0, k = 0; i < 13; i += 1) {
    const line = [];
    for (let j = 0; j < 14; j += 1, k += 1) {
      line.push(map[k] === "0" ? 0 : 1);
    }
    graph.push(line);
  }
  return graph;
};

/**
 * Handles openRecordContent.
 * ??openRecordContent?
 */
const openRecordContent = (recordId) => {
  if (!recordId) return;
  const targetRecord = records.value.find((item) => item.record.id === recordId);
  if (!targetRecord || !targetRecord.record?.map) return;

  recordStore.updateIsRecord(true);
  pkStore.updateGame({
    map: stringTo2D(targetRecord.record.map),
    a_id: targetRecord.record.aid,
    a_sx: targetRecord.record.asx,
    a_sy: targetRecord.record.asy,
    b_id: targetRecord.record.bid,
    b_sx: targetRecord.record.bsx,
    b_sy: targetRecord.record.bsy,
  });
  recordStore.updateSteps({
    a_steps: targetRecord.record.asteps,
    b_steps: targetRecord.record.bsteps,
  });
  recordStore.updateRecordLoser(targetRecord.record.loser);

  router.push({
    name: "record_content",
    params: { recordId },
  });
};

onMounted(() => {
  hasMounted.value = true;
  pullPage(currentPage.value, { force: true });
});

onActivated(() => {
  if (!hasMounted.value) return;
  pullPage(currentPage.value, { force: true });
});
</script>

<style scoped>
.record-view {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.view-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.stats-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(61, 174, 255, 0.12);
  color: var(--kob-accent-strong);
  font-weight: 700;
}

.list-table {
  margin: 0;
  min-width: 760px;
  --bs-table-bg: transparent;
  --bs-table-striped-bg: rgba(0, 0, 0, 0.02);
  --bs-table-hover-bg: rgba(90, 180, 255, 0.1);
}

.username {
  margin-left: 10px;
  font-weight: 600;
  color: var(--kob-text);
}

.replay-btn {
  min-height: 38px;
  padding-inline: 16px;
}

.skeleton-line {
  width: 100%;
  height: 22px;
  border-radius: 999px;
}

.empty-row {
  color: var(--kob-muted);
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.page-btn {
  cursor: pointer;
}

.page-btn:disabled {
  cursor: not-allowed;
}

@media (max-width: 991px) {
  .view-head {
    flex-direction: column;
    align-items: stretch;
  }

  .list-table {
    min-width: 680px;
  }

  .pager-wrap {
    justify-content: center;
  }
}
</style>
