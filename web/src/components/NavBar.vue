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
            <router-link :class="routeName === 'pk_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'pk_index' }">PK</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="routeName === 'record_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'record_index' }">Records</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="routeName === 'ranklist_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'ranklist_index' }">Ranklist</router-link>
          </li>
          <li class="nav-item" v-if="store.state.user.is_login">
            <router-link :class="routeName === 'user_bot_index' ? 'nav-link kob-link active' : 'nav-link kob-link'" :to="{ name: 'user_bot_index' }">My Bot</router-link>
          </li>
        </ul>
        <ul class="navbar-nav" v-if="store.state.user.is_login">
          <li ref="userMenuRef" class="nav-item user-menu">
            <button
              class="nav-link kob-user-chip user-menu-trigger"
              type="button"
              @click="toggleUserMenu"
            >
              {{ store.state.user.username }}
            </button>
            <ul v-show="isUserMenuOpen" class="kob-dropdown-menu">
              <li>
                <button class="dropdown-item" type="button" @click="goMyBots">My Bots</button>
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li><button class="dropdown-item" type="button" @click="logout">Log Out</button></li>
            </ul>
          </li>
        </ul>
        <ul class="navbar-nav" v-else-if="!store.state.user.pulling_info">
          <li class="nav-item">
            <router-link class="nav-link kob-link" :to="{ name: 'user_account_login' }" role="button">
              Log In
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link kob-link" :to="{ name: 'user_account_register' }" role="button">
              Register
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
import { useStore } from "vuex";

const store = useStore();
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
  await store.dispatch("logout");
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
  border: 1px solid rgba(145, 210, 255, 0.18);
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(7, 22, 36, 0.86), rgba(12, 30, 45, 0.8));
  backdrop-filter: blur(10px);
}

.kob-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #e6f4ff;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.kob-brand:hover {
  color: #ffffff;
}

.brand-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5ad1ff, #ffd05d);
  box-shadow: 0 0 14px rgba(90, 209, 255, 0.75);
}

.kob-link {
  margin-right: 6px;
  border-radius: 999px;
  color: #d4e8f8;
  transition: all 180ms ease;
}

.kob-link:hover,
.kob-link.active {
  color: #ffffff;
  background: rgba(90, 209, 255, 0.2);
}

.kob-user-chip {
  border: 1px solid rgba(90, 209, 255, 0.32);
  border-radius: 999px;
  padding: 6px 12px;
  color: #e6f4ff;
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
  border: 1px solid rgba(145, 210, 255, 0.24);
  background: rgba(12, 30, 45, 0.98);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.34);
}

.kob-dropdown-menu .dropdown-item {
  display: block;
  width: 100%;
  border: 0;
  text-align: left;
  background: transparent;
  color: #d8ecfc;
  padding: 8px 14px;
}

.kob-dropdown-menu .dropdown-item:hover {
  background: rgba(90, 209, 255, 0.18);
}
</style>
