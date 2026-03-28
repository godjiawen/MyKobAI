import { createRouter, createWebHistory } from "vue-router";
import store from "@/store";

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
    meta: { requiresAuth: true },
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
    meta: { requiresAuth: true },
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

router.beforeEach(async (to) => {
  if (!authInitialized) {
    await store.dispatch("initAuth");
    authInitialized = true;
  }

  if (to.meta.requiresAuth && !store.state.user.is_login) {
    return { name: "user_account_login" };
  }

  return true;
});

export default router;
