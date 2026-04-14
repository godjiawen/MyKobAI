<!-- 界面组件。 -->
<template>
  <ContentField>
    <section class="list-panel">
      <div class="panel-header">
        <h2>对局记录</h2>
        <p>回放历史对局，复盘每一步决策。</p>
      </div>
      <div class="table-wrap">
        <table class="table table-striped table-hover list-table" style="text-align: center;" :aria-busy="loading">
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
            <tr v-for="row in skeletonRows" :key="`record-skeleton-${row}`" class="skeleton-row">
              <td colspan="5">
                <div class="skeleton-line"></div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="record in records" :key="record.id">
              <td>
                <img :src="record.a_photo || fallbackPhoto" alt="" class="record-user-photo" />
                &nbsp;
                <span class="record-user-username">{{ record.a_username || "-" }}</span>
              </td>
              <td>
                <img :src="record.b_photo || fallbackPhoto" alt="" class="record-user-photo" />
                &nbsp;
                <span class="record-user-username">{{ record.b_username || "-" }}</span>
              </td>
              <td>{{ record.result || "-" }}</td>
              <td>{{ record.record?.createtime || "-" }}</td>
              <td>
                <button @click="openRecordContent(record.record?.id)" type="button" class="btn btn-secondary replay-btn">
                  回放
                </button>
              </td>
            </tr>
            <tr v-if="!errorMessage && records.length === 0">
              <td colspan="5" class="empty-row">暂无回放记录</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="loading" class="state-tip">回放列表加载中...</p>
      <p v-if="errorMessage" class="state-tip state-error">{{ errorMessage }}</p>
      <nav aria-label="pagination" class="pager-wrap">
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
import { computed, onMounted, ref } from "vue";
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
const skeletonRows = [1, 2, 3, 4, 5];
const fallbackPhoto = defaultAvatar;

const maxPages = computed(() => Math.ceil(totalRecords.value / 10));
const canPrev = computed(() => currentPage.value > 1);
const canNext = computed(() => currentPage.value < maxPages.value);

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

const clickPage = (page) => {
  if (loading.value) return;

  let target = page;
  if (target === -2) target = currentPage.value - 1;
  if (target === -1) target = currentPage.value + 1;

  if (target >= 1 && target <= maxPages.value) {
    pullPage(target);
  }
};

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
  pullPage(currentPage.value, { force: true });
});
</script>

<style scoped>
.panel-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  color: var(--kob-text);
}

.panel-header p {
  margin: 0;
  color: var(--kob-muted);
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.table-wrap {
  overflow-x: auto;
}

.table-wrap .table {
  min-width: 720px;
}

.page-btn {
  cursor: pointer;
}

.page-btn:disabled {
  cursor: not-allowed;
}

.skeleton-row td {
  padding-top: 12px;
  padding-bottom: 12px;
}

.skeleton-line {
  width: 100%;
  height: 22px;
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(90, 180, 255, 0.12), rgba(90, 180, 255, 0.28), rgba(90, 180, 255, 0.12));
  background-size: 240% 100%;
  animation: skeleton-wave 1.25s ease-in-out infinite;
}

.replay-btn {
  border-radius: 999px;
  background-color: var(--kob-accent);
  border: none;
  color: #fff;
}

.replay-btn:hover {
  background-color: var(--kob-accent-strong);
}

img.record-user-photo {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(90, 180, 255, 0.45);
}

.record-user-username {
  font-weight: 600;
  color: var(--kob-text);
}

.state-tip {
  margin: 10px 0 0;
  color: var(--kob-muted);
}

.state-error {
  color: #e74c3c;
}

.empty-row {
  color: var(--kob-muted);
}

@keyframes skeleton-wave {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}

@media (max-width: 991px) {
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .table-wrap .table {
    min-width: 660px;
    font-size: 0.92rem;
  }

  .record-user-username {
    font-size: 0.88rem;
  }

  .pager-wrap {
    justify-content: center;
  }
}
</style>
