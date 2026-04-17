<template>
  <nav class="navbar navbar-expand-lg kob-navbar">
    <div class="container">
      <router-link class="navbar-brand kob-brand" :to="{ name: 'home' }">
        <span class="brand-dot"></span>
        King Of Bots
      </router-link>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarText"
        aria-controls="navbarText"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarText">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <router-link :class="routeName === 'pk_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'pk_index' }">
              PK
            </router-link>
          </li>
          <li class="nav-item">
            <router-link :class="routeName === 'friends_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'friends_index' }">
              好友
            </router-link>
          </li>
          <li class="nav-item">
            <router-link :class="routeName === 'record_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'record_index' }">
              对局记录
            </router-link>
          </li>
          <li class="nav-item">
            <router-link :class="routeName === 'ranklist_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'ranklist_index' }">
              排行榜
            </router-link>
          </li>
        </ul>
        <ul class="navbar-nav" v-if="userStore.is_login">
          <li ref="userMenuRef" class="nav-item user-menu">
            <button
              class="nav-link kob-user-chip user-menu-trigger"
              type="button"
              @click="toggleUserMenu"
            >
              {{ userStore.username }}
            </button>
            <ul v-show="isUserMenuOpen" class="kob-dropdown-menu">
              <li>
                <button class="dropdown-item" type="button" @click="goMyBots">我的 Bot</button>
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li><button class="dropdown-item" type="button" @click="logout">退出登录</button></li>
            </ul>
          </li>
        </ul>
        <ul class="navbar-nav" v-else-if="!userStore.pulling_info">
          <li class="nav-item">
            <router-link class="nav-link kob-link" :to="{ name: 'user_account_login' }" role="button">
              登录
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link kob-link" :to="{ name: 'user_account_register' }" role="button">
              注册
            </router-link>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();

const routeName = computed(() => route.name);
const isUserMenuOpen = ref(false);
const userMenuRef = ref(null);

const closeUserMenu = () => {
  isUserMenuOpen.value = false;
};

const toggleUserMenu = () => {
  isUserMenuOpen.value = !isUserMenuOpen.value;
};

const handleOutsideClick = (event) => {
  if (!isUserMenuOpen.value) return;
  if (!userMenuRef.value?.contains(event.target)) {
    closeUserMenu();
  }
};

const goMyBots = async () => {
  closeUserMenu();
  await router.push({ name: "user_bot_index" });
};

const logout = async () => {
  closeUserMenu();
  await userStore.logout();
  await router.push({ name: "user_account_login" });
};

onMounted(() => {
  document.addEventListener("click", handleOutsideClick);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleOutsideClick);
});
</script>

<style scoped>
.kob-navbar {
  position: relative;
  z-index: 2100;
  margin: 16px 14px 0;
  padding: 10px 6px;
  border: 1px solid rgba(90, 180, 255, 0.3);
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.95), rgba(240, 248, 255, 0.85));
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 20px rgba(0, 50, 100, 0.05);
}

.kob-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--kob-text);
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.kob-brand:hover {
  color: var(--kob-accent-strong);
}

.brand-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3daeff, #ffd05d);
  box-shadow: 0 0 10px rgba(61, 174, 255, 0.6);
}

.kob-link {
  margin-right: 6px;
  border-radius: 999px;
  color: var(--kob-muted);
  transition:
    color 180ms ease,
    background-color 180ms ease,
    transform 180ms ease;
}

.kob-link:hover,
.kob-link.active {
  color: var(--kob-accent-strong);
  background: rgba(61, 174, 255, 0.1);
}

.kob-link:hover {
  transform: translateY(-1px);
}

.kob-user-chip {
  border: 1px solid rgba(90, 180, 255, 0.5);
  border-radius: 999px;
  padding: 6px 12px;
  color: var(--kob-text);
  background: transparent;
}

.user-menu {
  position: relative;
}

.user-menu-trigger {
  border: 0;
}

.user-menu-trigger::after {
  margin-left: 0.45rem;
}

.kob-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 170px;
  z-index: 2200;
  padding: 8px 0;
  margin: 0;
  list-style: none;
  border-radius: 12px;
  border: 1px solid rgba(90, 180, 255, 0.3);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 28px rgba(0, 50, 100, 0.1);
}

.kob-dropdown-menu .dropdown-item {
  display: block;
  width: 100%;
  border: 0;
  text-align: left;
  background: transparent;
  color: var(--kob-text);
  padding: 8px 14px;
}

.kob-dropdown-menu .dropdown-item:hover {
  background: rgba(61, 174, 255, 0.1);
  color: var(--kob-accent-strong);
}
</style>
