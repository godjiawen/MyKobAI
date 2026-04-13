<template>
  <ContentField>
    <section class="list-panel">
      <div class="panel-header">
        <h2>Ranklist</h2>
        <p>Top players by rating.</p>
      </div>
      <table class="table table-striped table-hover" style="text-align: center;">
        <thead>
          <tr>
            <th>Player</th>
            <th>Rating</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>
              <img :src="user.photo || fallbackPhoto" alt="" class="record-user-photo" />
              &nbsp;
              <span class="record-user-username">{{ user.username || '-' }}</span>
            </td>
            <td>{{ user.rating ?? 0 }}</td>
          </tr>
          <tr v-if="!loading && !errorMessage && users.length === 0">
            <td colspan="2" class="empty-row">暂无排行榜数据</td>
          </tr>
        </tbody>
      </table>
      <p v-if="loading" class="state-tip">排行榜加载中...</p>
      <p v-if="errorMessage" class="state-tip state-error">{{ errorMessage }}</p>
      <nav aria-label="pagination" class="pager-wrap">
        <ul class="pagination">
          <li class="page-item" @click.prevent="clickPage(-2)">
            <a class="page-link" href="#">Prev</a>
          </li>
          <li :class="`page-item ${page.is_active}`" v-for="page in pages" :key="page.number" @click.prevent="clickPage(page.number)">
            <a class="page-link" href="#">{{ page.number }}</a>
          </li>
          <li class="page-item" @click.prevent="clickPage(-1)">
            <a class="page-link" href="#">Next</a>
          </li>
        </ul>
      </nav>
    </section>
  </ContentField>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useUserStore } from "@/store/user";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const userStore = useUserStore();

const users = ref([]);
const pages = ref([]);
const loading = ref(false);
const errorMessage = ref("");
let currentPage = 1;
let totalUsers = 0;
const fallbackPhoto = "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png";

const updatePages = () => {
  const maxPages = Math.ceil(totalUsers / 10);
  const nextPages = [];

  for (let i = currentPage - 2; i <= currentPage + 2; i += 1) {
    if (i >= 1 && i <= maxPages) {
      nextPages.push({
        number: i,
        is_active: i === currentPage ? "active" : "",
      });
    }
  }

  pages.value = nextPages;
};

const pullPage = async (page) => {
  currentPage = page;
  loading.value = true;
  errorMessage.value = "";
  try {
    const resp = await apiRequest(API_PATHS.ranklist, {
      data: { page },
      token: userStore.token,
    });
    users.value = Array.isArray(resp.users) ? resp.users : [];
    totalUsers = resp.users_count || 0;
    updatePages();
  } catch (error) {
    console.error(error);
    users.value = [];
    totalUsers = 0;
    pages.value = [];
    errorMessage.value = "排行榜加载失败，请确认后端接口是否可用。";
  } finally {
    loading.value = false;
  }
};

const clickPage = (page) => {
  let target = page;
  if (target === -2) target = currentPage - 1;
  if (target === -1) target = currentPage + 1;

  const maxPages = Math.ceil(totalUsers / 10);
  if (target >= 1 && target <= maxPages) {
    pullPage(target);
  }
};

onMounted(() => {
  pullPage(currentPage);
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
</style>

