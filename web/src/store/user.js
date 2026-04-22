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
    /**
     * 处理 updateUser 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateUser, including state updates, interaction orchestration, and error branches.
     */
    updateUser(user) {
      this.id = user.id;
      this.username = user.username;
      this.photo = user.photo;
      this.is_login = user.is_login;
    },
    /**
     * 处理 updateToken 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateToken, including state updates, interaction orchestration, and error branches.
     */
    updateToken(token) {
      this.token = token;
    },
    /**
     * 处理 updatePhoto 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updatePhoto, including state updates, interaction orchestration, and error branches.
     */
    updatePhoto(photoUrl) {
      this.photo = photoUrl;
    },
    /**
     * 处理 updateUsername 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updateUsername, including state updates, interaction orchestration, and error branches.
     */
    updateUsername(newUsername) {
      this.username = newUsername;
    },
    /**
     * 处理 clearUserSession 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of clearUserSession, including state updates, interaction orchestration, and error branches.
     */
    clearUserSession() {
      this.id = "";
      this.photo = "";
      this.username = "";
      this.token = "";
      this.is_login = false;
    },
    /**
     * 处理 updatePullingInfo 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of updatePullingInfo, including state updates, interaction orchestration, and error branches.
     */
    updatePullingInfo(pullingInfo) {
      this.pulling_info = pullingInfo;
    },
    /**
     * 处理 login 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of login with async flow control, including state updates, interaction orchestration, and error branches.
     *
     * @param username, password 输入参数；Input parameter.
     */
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

      try {
        return await this.getinfo();
      } catch (error) {
        localStorage.removeItem("jwt_token");
        this.clearUserSession();
        throw error;
      }
    },
    /**
     * 处理 getinfo 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of getinfo with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
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
    /**
     * 处理 logout 的核心前端逻辑，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of logout, including state updates, interaction orchestration, and error branches.
     */
    logout() {
      localStorage.removeItem("jwt_token");
      this.clearUserSession();
    },
    /**
     * 处理 initAuth 的核心前端逻辑，并包含异步流程控制，负责状态更新、交互调度与异常分支处理。
     * Handles the core frontend logic of initAuth with async flow control, including state updates, interaction orchestration, and error branches.
     *
     */
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

