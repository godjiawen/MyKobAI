import { createRouter, createWebHistory } from "vue-router";
import pinia from "@/store";
import { useUserStore } from "@/store/user";

const routes = [
  {
    path: "/",
    name: "home",
    redirect: "/pk/",
    meta: { requiresAuth: true },
  },
  {
    path: "/pk/",
    name: "pk_index",
    component: () => import("@/views/pk/PkIndexView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/record/",
    name: "record_index",
    component: () => import("@/views/record/RecordIndexView.vue"),
    meta: { requiresAuth: true, keepAlive: true },
  },
  {
    path: "/record/:recordId/",
    name: "record_content",
    component: () => import("@/views/record/RecordContentView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/ranklist/",
    name: "ranklist_index",
    component: () => import("@/views/ranklist/RanklistIndexView.vue"),
    meta: { requiresAuth: true, keepAlive: true },
  },
  {
    path: "/user/account/login/",
    name: "user_account_login",
    component: () => import("@/views/user/account/UserAccountLoginView.vue"),
  },
  {
    path: "/user/account/register/",
    name: "user_account_register",
    component: () => import("@/views/user/account/UserAccountRegisterView.vue"),
  },
  {
    path: "/user/bot/",
    name: "user_bot_index",
    component: () => import("@/views/user/bot/UserBotIndexView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/404/",
    name: "404",
    component: () => import("@/views/error/NotFound.vue"),
  },
  {
    path: "/:catchAll(.*)",
    redirect: "/404/",
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

let authInitialized = false;
let authInitPromise = null;
const userStore = useUserStore(pinia);

router.beforeEach(async (to) => {
  // 首次路由跳转时仅初始化一次鉴权态，避免并发重复请求 用户信息接口。
  if (!authInitialized) {
    if (!authInitPromise) {
      authInitPromise = userStore.initAuth().finally(() => {
        authInitialized = true;
        authInitPromise = null;
      });
    }
    await authInitPromise;
  }

  if (to.meta.requiresAuth && !userStore.is_login) {
    // 受保护页面在未登录时统一跳转登录页。
    return { name: "user_account_login" };
  }

  return true;
});

export default router;

