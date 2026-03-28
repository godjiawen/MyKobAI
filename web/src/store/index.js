import { createStore } from "vuex";
import user from "./user";
import pk from "./pk";
import record from "./record";

export default createStore({
  modules: {
    user,
    pk,
    record,
  },
});
