<template>
  <ContentField>
    <section class="list-panel">
      <div class="panel-header">
        <h2>Battle Records</h2>
        <p>Replay any historical match.</p>
      </div>
      <table class="table table-striped table-hover" style="text-align: center;">
        <thead>
          <tr>
            <th>Player A</th>
            <th>Player B</th>
            <th>Result</th>
            <th>Time</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
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
                Replay
              </button>
            </td>
          </tr>
          <tr v-if="!loading && !errorMessage && records.length === 0">
            <td colspan="5" class="empty-row">暂无回放记录</td>
          </tr>
        </tbody>
      </table>
      <p v-if="loading" class="state-tip">回放列表加载中...</p>
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
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const store = useStore();
const router = useRouter();

const records = ref([]);
const pages = ref([]);
const loading = ref(false);
const errorMessage = ref("");
let currentPage = 1;
let totalRecords = 0;
const fallbackPhoto = "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png";

const updatePages = () => {
  const maxPages = Math.ceil(totalRecords / 10);
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
    const resp = await apiRequest(API_PATHS.records, {
      data: { page },
      token: store.state.user.token,
    });
    const list = Array.isArray(resp.records) ? resp.records : [];
    records.value = list.map((item) => ({
      ...item,
      record: item.record || {},
    }));
    totalRecords = resp.records_count || 0;
    updatePages();
  } catch (error) {
    console.error(error);
    records.value = [];
    totalRecords = 0;
    pages.value = [];
    errorMessage.value = "回放列表加载失败，请确认后端接口是否可用。";
  } finally {
    loading.value = false;
  }
};

const clickPage = (page) => {
  let target = page;
  if (target === -2) target = currentPage - 1;
  if (target === -1) target = currentPage + 1;

  const maxPages = Math.ceil(totalRecords / 10);
  if (target >= 1 && target <= maxPages) {
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

  store.commit("updateIsRecord", true);
  store.commit("updateGame", {
    map: stringTo2D(targetRecord.record.map),
    a_id: targetRecord.record.aid,
    a_sx: targetRecord.record.asx,
    a_sy: targetRecord.record.asy,
    b_id: targetRecord.record.bid,
    b_sx: targetRecord.record.bsx,
    b_sy: targetRecord.record.bsy,
  });
  store.commit("updateSteps", {
    a_steps: targetRecord.record.asteps,
    b_steps: targetRecord.record.bsteps,
  });
  store.commit("updateRecordLoser", targetRecord.record.loser);

  router.push({
    name: "record_content",
    params: { recordId },
  });
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
}

.panel-header p {
  margin: 0;
  color: #a9bfd3;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.replay-btn {
  border-radius: 999px;
}

img.record-user-photo {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(145, 210, 255, 0.45);
}

.record-user-username {
  font-weight: 600;
}

.state-tip {
  margin: 10px 0 0;
  color: #a9bfd3;
}

.state-error {
  color: #ff8f8f;
}

.empty-row {
  color: #a9bfd3;
}
</style>
