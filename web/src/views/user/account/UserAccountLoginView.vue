<!-- 界面组件。 -->
<template>
  <ContentField v-if="!userStore.pulling_info">
    <section class="auth-wrap">
      <div class="auth-head">
        <h2>欢迎回来</h2>
        <p>登录后即可继续开始对局。</p>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-lg-5 col-md-7 col-12">
          <form class="auth-form" @submit.prevent="login">
            <div class="mb-3">
              <label for="username" class="form-label">用户名</label>
              <input id="username" v-model="username" type="text" class="form-control" placeholder="请输入用户名" />
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input id="password" v-model="password" type="password" class="form-control" placeholder="请输入密码" />
            </div>
            <div class="error-message">{{ errorMessage }}</div>
            <button type="submit" class="btn btn-primary auth-btn">登录</button>
          </form>
        </div>
      </div>
    </section>
  </ContentField>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/store/user";
import ContentField from "@/components/ContentField.vue";

const userStore = useUserStore();
const router = useRouter();

const username = ref("");
const password = ref("");
const errorMessage = ref("");

const login = async () => {
  errorMessage.value = "";
  try {
    await userStore.login({
      username: username.value,
      password: password.value,
    });
    await userStore.getinfo();
    await router.push({ name: "home" });
  } catch (error) {
    errorMessage.value = error.message || "用户名或密码错误";
  }
};
</script>

<style scoped>
.auth-wrap {
  padding: 6px 4px;
}

.auth-head {
  text-align: center;
  margin-bottom: 20px;
}

.auth-head h2 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
  font-weight: 700;
  color: var(--kob-text);
}

.auth-head p {
  margin: 8px 0 0;
  color: var(--kob-muted);
}

.auth-form {
  padding: 20px;
  border: 1px solid rgba(90, 180, 255, 0.3);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.6);
}

.auth-btn {
  width: 100%;
  margin-top: 4px;
  border-radius: 12px;
  padding: 10px 0;
  font-weight: 600;
  color: #ffffff;
}

div.error-message {
  min-height: 22px;
  color: #e74c3c;
}
</style>

