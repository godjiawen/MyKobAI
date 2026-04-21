<template>
  <section class="record-content-wrap">
    <div class="record-content-head">
      <button type="button" class="back-btn" @click="goBackToRecordList">
        返回记录列表
      </button>
      <div class="head-copy">
        <h2>对局回放</h2>
        <p>按时间顺序查看这场对局的完整过程。</p>
      </div>
    </div>
    <div v-if="loadError" class="state-error">{{ loadError }}</div>
    <div v-else-if="loading" class="state-tip">录像加载中...</div>
    <PlayGround v-else-if="ready" />
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/store/user";
import { usePkStore } from "@/store/pk";
import { useRecordStore } from "@/store/record";
import PlayGround from "@/components/PlayGround.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const userStore = useUserStore();
const pkStore = usePkStore();
const recordStore = useRecordStore();
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const loadError = ref("");
const ready = ref(false);

const goBackToRecordList = async () => {
  await router.push({ name: "record_index" });
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

onMounted(async () => {
  if (pkStore.gamemap) {
    recordStore.updateIsRecord(true);
    ready.value = true;
    return;
  }

  const recordId = route.params.recordId;
  if (!recordId) {
    loadError.value = "无效的录像 ID";
    return;
  }

  loading.value = true;
  try {
    const resp = await apiRequest(API_PATHS.recordDetail, {
      data: { record_id: recordId },
      token: userStore.token,
    });
    if (resp.error_message !== "success" || !resp.record?.map) {
      loadError.value = "未找到对应录像，请返回列表重新选择。";
      return;
    }

    const record = resp.record;
    recordStore.updateIsRecord(true);
    pkStore.updateGame({
      map: stringTo2D(record.map),
      a_id: record.aid,
      a_sx: record.asx,
      a_sy: record.asy,
      b_id: record.bid,
      b_sx: record.bsx,
      b_sy: record.bsy,
    });
    recordStore.updateSteps({ a_steps: record.asteps, b_steps: record.bsteps });
    recordStore.updateRecordLoser(record.loser);
    ready.value = true;
  } catch (error) {
    console.error(error);
    loadError.value = "录像加载失败，请确认后端接口是否可用。";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.record-content-wrap {
  width: min(1080px, 96vw);
  margin: 20px auto 0;
  animation: reveal 260ms ease;
}

.record-content-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  margin-bottom: 12px;
}

.head-copy {
  text-align: center;
  grid-column: 2;
}

.record-content-head h2 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  color: var(--kob-text);
}

.record-content-head p {
  margin: 6px 0 0;
  color: var(--kob-muted);
}

.back-btn {
  grid-column: 1;
  justify-self: start;
  border: 1px solid rgba(90, 180, 255, 0.45);
  border-radius: 999px;
  padding: 8px 14px;
  color: var(--kob-text);
  background: rgba(255, 255, 255, 0.72);
  font-weight: 600;
  line-height: 1;
  transition: all 160ms ease;
}

.back-btn:hover {
  color: var(--kob-accent-strong);
  border-color: rgba(90, 180, 255, 0.7);
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.state-tip {
  text-align: center;
  margin: 40px 0;
  color: var(--kob-muted);
}

.state-error {
  text-align: center;
  margin: 40px 0;
  color: #e74c3c;
}

@keyframes reveal {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .record-content-head {
    grid-template-columns: 1fr;
    gap: 10px;
    justify-items: center;
  }

  .back-btn,
  .head-copy {
    grid-column: 1;
  }

  .back-btn {
    justify-self: center;
  }
}
</style>
