<template>
  <ContentField>
    <section class="ranklist-view">
      <header class="view-head">
        <div>
          <p class="kob-kicker">Leaderboard</p>
          <h2 class="kob-headline">排行榜</h2>
          <p class="kob-subtitle">按积分查看当前最强玩家，支持分页浏览。</p>
        </div>
        <span class="stats-badge">总玩家 {{ totalUsers }}</span>
      </header>

      <div class="kob-table-wrap">
        <table class="table table-hover align-middle rank-table" :aria-busy="loading">
          <thead>
            <tr>
              <th>排名</th>
              <th>玩家</th>
              <th>积分</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="row in skeletonRows" :key="`rank-skeleton-${row}`">
              <td colspan="3">
                <div class="skeleton-line kob-shimmer"></div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="(user, index) in users" :key="user.id">
              <td>
                <span class="rank-index">{{ (currentPage - 1) * 10 + index + 1 }}</span>
              </td>
              <td>
                <img :src="user.photo || fallbackPhoto" alt="" class="kob-avatar-36" />
                <span class="username">{{ user.username || "-" }}</span>
              </td>
              <td>{{ user.rating ?? 0 }}</td>
            </tr>
            <tr v-if="!errorMessage && users.length === 0">
              <td colspan="3" class="empty-row">暂无排行榜数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="loading" class="kob-status-inline">排行榜加载中...</p>
      <p v-if="errorMessage" class="kob-status-inline kob-status-error">{{ errorMessage }}</p>

      <nav aria-label="排行榜分页" class="pager-wrap">
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
  name: "RanklistIndexView",
});

import { computed, onActivated, onMounted, ref } from "vue";
import { useUserStore } from "@/store/user";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";
import defaultAvatar from "@/assets/images/default-avatar.svg";

const userStore = useUserStore();

const users = ref([]);
const pages = ref([]);
const loading = ref(true);
const errorMessage = ref("");
const currentPage = ref(1);
const totalUsers = ref(0);
const hasMounted = ref(false);
const skeletonRows = [1, 2, 3, 4, 5];
const fallbackPhoto = defaultAvatar;

const maxPages = computed(() => Math.ceil(totalUsers.value / 10));
const canPrev = computed(() => currentPage.value > 1);
const canNext = computed(() => currentPage.value < maxPages.value);

/**
 * 处理 updatePages 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of updatePages, including state updates, interaction orchestration, and error branches.
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

/**
 * 处理 pullPage 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of pullPage with async flow control, including state updates, interaction orchestration, and error branches.
 *
 * @param page 输入参数；Input parameter.
 * @param force 输入参数；Input parameter.
 */
const pullPage = async (page, { force = false } = {}) => {
  if (loading.value && !force) return;

  currentPage.value = page;
  loading.value = true;
  errorMessage.value = "";
  try {
    const resp = await apiRequest(API_PATHS.ranklist, {
      data: { page },
      token: userStore.token,
    });
    users.value = Array.isArray(resp.users) ? resp.users : [];
    totalUsers.value = resp.users_count || 0;
    updatePages();
  } catch (error) {
    console.error(error);
    users.value = [];
    totalUsers.value = 0;
    pages.value = [];
    errorMessage.value = "排行榜加载失败，请确认后端接口是否可用。";
  } finally {
    loading.value = false;
  }
};

/**
 * 处理 clickPage 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
 * Handles the core frontend logic of clickPage, including state updates, interaction orchestration, and error branches.
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
.ranklist-view {
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

.rank-table {
  margin: 0;
  --bs-table-bg: transparent;
  --bs-table-striped-bg: rgba(0, 0, 0, 0.02);
  --bs-table-hover-bg: rgba(90, 180, 255, 0.1);
}

.rank-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  min-height: 30px;
  border-radius: 999px;
  background: rgba(255, 183, 50, 0.15);
  color: #9f6500;
  font-weight: 700;
}

.username {
  margin-left: 10px;
  font-weight: 600;
  color: var(--kob-text);
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

  .pager-wrap {
    justify-content: center;
  }
}
</style>
