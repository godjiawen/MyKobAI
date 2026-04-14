<!-- 界面组件。 -->
<template>
  <ContentField>
    <section class="list-panel">
      <div class="panel-header">
        <h2>排行榜</h2>
        <p>按积分查看当前顶尖玩家。</p>
      </div>
      <div class="table-wrap">
        <table class="table table-striped table-hover" style="text-align: center;" :aria-busy="loading">
          <thead>
            <tr>
              <th>玩家</th>
              <th>积分</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr v-for="row in skeletonRows" :key="`rank-skeleton-${row}`" class="skeleton-row">
              <td colspan="2">
                <div class="skeleton-line"></div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="user in users" :key="user.id">
              <td>
                <img :src="user.photo || fallbackPhoto" alt="" class="record-user-photo" />
                &nbsp;
                <span class="record-user-username">{{ user.username || '-' }}</span>
              </td>
              <td>{{ user.rating ?? 0 }}</td>
            </tr>
            <tr v-if="!errorMessage && users.length === 0">
              <td colspan="2" class="empty-row">暂无排行榜数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="loading" class="state-tip">排行榜加载中...</p>
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
const skeletonRows = [1, 2, 3, 4, 5];
const fallbackPhoto = defaultAvatar;

const maxPages = computed(() => Math.ceil(totalUsers.value / 10));
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
  min-width: 420px;
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
    min-width: 380px;
    font-size: 0.92rem;
  }

  .record-user-username {
    font-size: 0.9rem;
  }

  .pager-wrap {
    justify-content: center;
  }
}
</style>
