import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

export default {
  state: () => ({
    id: "",
    username: "",
    photo: "",
    token: "",
    is_login: false,
    pulling_info: true,
  }),
  mutations: {
    updateUser(state, user) {
      state.id = user.id;
      state.username = user.username;
      state.photo = user.photo;
      state.is_login = user.is_login;
    },
    updateToken(state, token) {
      state.token = token;
    },
    updatePhoto(state, photoUrl) {
      state.photo = photoUrl;
    },
    updateUsername(state, newUsername) {
      state.username = newUsername;
    },
    logout(state) {
      state.id = "";
      state.photo = "";
      state.username = "";
      state.token = "";
      state.is_login = false;
    },
    updatePullingInfo(state, pullingInfo) {
      state.pulling_info = pullingInfo;
    },
  },
  actions: {
    async login({ commit }, { username, password }) {
      const resp = await apiRequest(API_PATHS.login, {
        method: "POST",
        data: { username, password },
      });

      if (resp.error_message !== "success") {
        throw new Error(resp.error_message || "Login failed");
      }

      localStorage.setItem("jwt_token", resp.token);
      commit("updateToken", resp.token);
      return resp;
    },

    async getinfo({ commit, state }) {
      const resp = await apiRequest(API_PATHS.userInfo, {
        token: state.token,
      });

      if (resp.error_message !== "success") {
        throw new Error(resp.error_message || "Get info failed");
      }

      commit("updateUser", {
        ...resp,
        is_login: true,
      });
      return resp;
    },

    logout({ commit }) {
      localStorage.removeItem("jwt_token");
      commit("logout");
    },

    async initAuth({ commit, dispatch }) {
      const token = localStorage.getItem("jwt_token");
      if (!token) {
        commit("updatePullingInfo", false);
        return false;
      }

      commit("updateToken", token);
      try {
        await dispatch("getinfo");
        return true;
      } catch (error) {
        localStorage.removeItem("jwt_token");
        commit("logout");
        return false;
      } finally {
        commit("updatePullingInfo", false);
      }
    },
  },
};
