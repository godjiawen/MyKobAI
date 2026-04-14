// 状态仓库模块。
import { defineStore } from "pinia";

export const useRecordStore = defineStore("record", {
  state: () => ({
    is_record: false,
    a_steps: "",
    b_steps: "",
    record_loser: "",
  }),
  actions: {
    updateIsRecord(isRecord) {
      this.is_record = isRecord;
    },
    updateSteps(data) {
      this.a_steps = data.a_steps;
      this.b_steps = data.b_steps;
    },
    updateRecordLoser(loser) {
      this.record_loser = loser;
    },
  },
});
