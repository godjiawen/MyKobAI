const trimTrailingSlash = (value) => value.replace(/\/+$/, "");

const apiBaseFromEnv = import.meta.env.VITE_API_BASE_URL;
const wsBaseFromEnv = import.meta.env.VITE_WS_BASE_URL;
const chatWsBaseFromEnv = import.meta.env.VITE_CHAT_WS_BASE_URL;

export const API_BASE_URL = trimTrailingSlash(apiBaseFromEnv || "http://127.0.0.1:3000");
export const WS_BASE_URL = trimTrailingSlash(
  wsBaseFromEnv || API_BASE_URL.replace(/^http/i, "ws")
);
// Chat system runs on port 3003 by default
export const CHAT_WS_BASE_URL = trimTrailingSlash(
  chatWsBaseFromEnv || WS_BASE_URL.replace(/:(\d+)$/, ":3003")
);

export const API_PATHS = {
  login: "/api/user/account/token/",
  register: "/api/user/account/register/",
  userInfo: "/api/user/account/info/",
  updateUsername: "/api/user/account/update/username/",
  updatePassword: "/api/user/account/update/password/",
  uploadAvatar: "/api/user/account/avatar/upload/",
  ranklist: "/api/ranklist/getlist/",
  records: "/api/record/getlist/",
  botList: "/api/user/bot/getlist/",
  botAdd: "/api/user/bot/add/",
  botUpdate: "/api/user/bot/update/",
  botRemove: "/api/user/bot/remove/",
};

export const buildWebSocketUrl = (token) => `${WS_BASE_URL}/websocket/${token}/`;
export const buildChatWebSocketUrl = (token, roomId) => `${CHAT_WS_BASE_URL}/chat/${token}/${roomId}`;
