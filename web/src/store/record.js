export default {
  state: () => ({
    is_record: false,
    a_steps: "",
    b_steps: "",
    record_loser: "",
  }),
  mutations: {
    updateIsRecord(state, isRecord) {
      state.is_record = isRecord;
    },
    updateSteps(state, data) {
      state.a_steps = data.a_steps;
      state.b_steps = data.b_steps;
    },
    updateRecordLoser(state, loser) {
      state.record_loser = loser;
    },
  },
};
