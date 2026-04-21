<template>
  <ContentField>
    <section class="auth-wrap">
      <div class="auth-head">
        <p class="kob-kicker">Register</p>
        <h2 class="kob-headline">创建账号</h2>
        <p class="kob-subtitle">完成注册后即可进入对局和好友系统。</p>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-lg-6 col-md-8 col-12">
          <form class="auth-form" @submit.prevent="register">
            <div class="mb-3">
              <label for="username" class="form-label">用户名</label>
              <input id="username" v-model="username" type="text" class="form-control" placeholder="请输入用户名" />
              <div class="rule-tip">6-12 位，仅允许字母、数字和下划线。</div>
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input id="password" v-model="password" type="password" class="form-control" placeholder="请输入密码" />
              <div class="rule-tip">8-16 位，至少包含大写/小写/数字/特殊字符中的两类。</div>
            </div>
            <div class="mb-3">
              <label for="confirmedPassword" class="form-label">确认密码</label>
              <input id="confirmedPassword" v-model="confirmedPassword" type="password" class="form-control" placeholder="请再次输入密码" />
            </div>
            <div class="error-message">{{ errorMessage }}</div>
            <button type="submit" class="btn btn-primary auth-btn">注册</button>
          </form>
        </div>
      </div>
    </section>
  </ContentField>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import ContentField from "@/components/ContentField.vue";
import { API_PATHS } from "@/config/env";
import { apiRequest } from "@/utils/http";

const router = useRouter();

const username = ref("");
const password = ref("");
const confirmedPassword = ref("");
const errorMessage = ref("");

/**
 * Handles validateRegisterForm.
 * ??validateRegisterForm?
 */
const validateRegisterForm = () => {
  const usernameValue = username.value.trim();
  const passwordValue = password.value;
  const confirmedValue = confirmedPassword.value;

  if (!/^[A-Za-z0-9_]{6,12}$/.test(usernameValue)) {
    return "用户名需为 6-12 位，仅允许字母、数字和下划线。";
  }

  if (passwordValue.length < 8 || passwordValue.length > 16) {
    return "密码长度需为 8-16 位。";
  }

  let score = 0;
  if (/[A-Z]/.test(passwordValue)) score += 1;
  if (/[a-z]/.test(passwordValue)) score += 1;
  if (/\d/.test(passwordValue)) score += 1;
  if (/[^A-Za-z0-9]/.test(passwordValue)) score += 1;
  if (score < 2) {
    return "密码至少包含大写字母、小写字母、数字、特殊字符中的两类。";
  }

  if (passwordValue !== confirmedValue) {
    return "两次输入的密码不一致。";
  }

  return "";
};

const register = async () => {
  errorMessage.value = "";

  const validationError = validateRegisterForm();
  if (validationError) {
    errorMessage.value = validationError;
    return;
  }

  try {
    const resp = await apiRequest(API_PATHS.register, {
      method: "POST",
      data: {
        username: username.value,
        password: password.value,
        confirmedPassword: confirmedPassword.value,
      },
    });

    if (resp.error_message !== "success") {
      errorMessage.value = resp.error_message || "注册失败";
      return;
    }

    await router.push({ name: "user_account_login" });
  } catch (error) {
    errorMessage.value = error.message || "注册失败";
  }
};
</script>

<style scoped>
.auth-wrap {
  padding: 8px 4px;
}

.auth-head {
  text-align: center;
  margin-bottom: 20px;
}

.auth-form {
  padding: 22px;
  border: 1px solid rgba(90, 180, 255, 0.28);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
}

.auth-btn {
  width: 100%;
  margin-top: 4px;
  border-radius: 12px;
  min-height: 44px;
  font-weight: 600;
  color: #ffffff;
}

div.error-message {
  min-height: 22px;
  color: #d14343;
}

.rule-tip {
  margin-top: 6px;
  color: var(--kob-muted);
  font-size: 0.8rem;
}
</style>
