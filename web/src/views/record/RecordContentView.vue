<template>
  <section class="record-content-wrap">
    <div class="record-content-head">
      <h2>Match Replay</h2>
      <p>Playback of a historical battle record.</p>
    </div>
    <div v-if="loadError" class="state-error">{{ loadError }}</div>
    <div v-else-if="loading" class="state-tip">录像加载中...</div>
    <PlayGround v-else-if="ready" />
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
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

const loading = ref(false);
const loadError = ref("");
const ready = ref(false);

const stringTo2D = (map) => {
  const graph = [];
  for (let i = 0, k = 0; i < 13; i++) {
    const line = [];
    for (let j = 0; j < 14; j++, k++) {
      line.push(map[k] === "0" ? 0 : 1);
    }
    graph.push(line);
  }
  return graph;
};

onMounted(async () => {
  // If store already has the map data (navigated from RecordIndexView), use it directly
  if (pkStore.gamemap) {
    recordStore.updateIsRecord(true);
    ready.value = true;
    return;
  }

  // Otherwise (direct URL / page refresh), fetch from backend by recordId
  const recordId = route.params.recordId;
  if (!recordId) {
    loadError.value = "无效的录像 ID";
    return;
  }

  loading.value = true;
  try {
    const resp = await apiRequest(API_PATHS.records, {
      data: { page: 1 },
      token: userStore.token,
    });
    const list = Array.isArray(resp.records) ? resp.records : [];
    const target = list.find((item) => String(item.record?.id) === String(recordId));

    if (!target || !target.record?.map) {
      loadError.value = "未找到对应录像，请返回列表重新选择。";
      return;
    }

    const r = target.record;
    recordStore.updateIsRecord(true);
    pkStore.updateGame({
      map: stringTo2D(r.map),
      a_id: r.aid,
      a_sx: r.asx,
      a_sy: r.asy,
      b_id: r.bid,
      b_sx: r.bsx,
      b_sy: r.bsy,
    });
    recordStore.updateSteps({ a_steps: r.asteps, b_steps: r.bsteps });
    recordStore.updateRecordLoser(r.loser);
    ready.value = true;
  } catch (e) {
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
  text-align: center;
  margin-bottom: 10px;
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
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

