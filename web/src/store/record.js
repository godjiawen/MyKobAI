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
     * 处理 updateIsRecord 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateIsRecord, including state updates, interaction orchestration, and error branches.
     */
    updateIsRecord(isRecord) {
      this.is_record = isRecord;
    },
    /**
     * 处理 updateSteps 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateSteps, including state updates, interaction orchestration, and error branches.
     */
    updateSteps(data) {
      this.a_steps = data.a_steps;
      this.b_steps = data.b_steps;
    },
    /**
     * 处理 updateRecordLoser 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateRecordLoser, including state updates, interaction orchestration, and error branches.
     */
    updateRecordLoser(loser) {
      this.record_loser = loser;
    },
  },
});
