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
    /**
     * Handles updateIsRecord.
     * ??updateIsRecord?
     */
    updateIsRecord(isRecord) {
      this.is_record = isRecord;
    },
    /**
     * Handles updateSteps.
     * ??updateSteps?
     */
    updateSteps(data) {
      this.a_steps = data.a_steps;
      this.b_steps = data.b_steps;
    },
    /**
     * Handles updateRecordLoser.
     * ??updateRecordLoser?
     */
    updateRecordLoser(loser) {
      this.record_loser = loser;
    },
  },
});
