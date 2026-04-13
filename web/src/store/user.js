import { defineStore } from "pinia";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const createDefaultState = () => ({
  id: "",
  username: "",
  photo: "",
  token: "",
  is_login: false,
  pulling_info: true,
});

export const useUserStore = defineStore("user", {
  state: createDefaultState,
  actions: {
    updateUser(user) {
      this.id = user.id;
      this.username = user.username;
      this.photo = user.photo;
      this.is_login = user.is_login;
    },
    updateToken(token) {
      this.token = token;
    },
    updatePhoto(photoUrl) {
      this.photo = photoUrl;
    },
    updateUsername(newUsername) {
      this.username = newUsername;
    },
    clearUserSession() {
      this.id = "";
      this.photo = "";
      this.username = "";
      this.token = "";
      this.is_login = false;
    },
    updatePullingInfo(pullingInfo) {
      this.pulling_info = pullingInfo;
    },
    async login({ username, password }) {
      const resp = await apiRequest(API_PATHS.login, {
        method: "POST",
        data: { username, password },
      });

      if (resp.error_message !== "success") {
        throw new Error(resp.error_message || "Login failed");
      }

      localStorage.setItem("jwt_token", resp.token);
      this.updateToken(resp.token);
      return resp;
    },
    async getinfo() {
      const resp = await apiRequest(API_PATHS.userInfo, {
        token: this.token,
      });

      if (resp.error_message !== "success") {
        throw new Error(resp.error_message || "Get info failed");
      }

      this.updateUser({
        ...resp,
        is_login: true,
      });
      return resp;
    },
    logout() {
      localStorage.removeItem("jwt_token");
      this.clearUserSession();
    },
    async initAuth() {
      const token = localStorage.getItem("jwt_token");
      if (!token) {
        this.updatePullingInfo(false);
        return false;
      }

      this.updateToken(token);
      try {
        await this.getinfo();
        return true;
      } catch (error) {
        localStorage.removeItem("jwt_token");
        this.clearUserSession();
        return false;
      } finally {
        this.updatePullingInfo(false);
      }
    },
  },
});
